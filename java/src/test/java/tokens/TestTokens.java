package tokens;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import common.*;
import objects.ObjectsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.model.*;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TestTokens {

    private final ApiClient apiClient = Client.create();
    private final ObjectsClient objectsClient = new ObjectsClient(apiClient);
    private final TokensClient tokensClient = new TokensClient(apiClient);
    private final CollectionSetup setup = new CollectionSetup();

    private final List<String> props = ImmutableList.of("id", "ssn", "email");
    private final List<String> tags = ImmutableList.of("token_tag_1");

    @BeforeEach()
    public void beforeEach() throws ApiException {
        setup.setUp();
    }

    @AfterEach()
    public void afterEach() throws ApiException {
        setup.tearDown();
    }

    @ParameterizedTest
    @MethodSource("reversibleTokenTypes")
    public void batchTokenizeAndBatchDetokenize(TokenType tokenType) throws ApiException {
        TokenizeResult tokenizeResult = batchTokenize(tokenType);
        DetokenizeResult detokenizedResult = batchDetokenize();

        assertDetokenizeResultIsCorrect(tokenizeResult, detokenizedResult);
    }

    @ParameterizedTest
    @MethodSource("reversibleTokenTypes")
    public void batchTokenizeAndSingleDetokenize(TokenType tokenType) throws ApiException {

        TokenizeResult tokenizeResult = batchTokenize(tokenType);
        DetokenizeResult detokenizeResult = singleDetokenize(tokenizeResult);

        assertDetokenizeResultIsCorrect(tokenizeResult, detokenizeResult);
    }

    @ParameterizedTest
    @MethodSource("tokenizationTypeAndArchived")
    public void cannotDetokenizeArchivedObjects(
        TokenType tokenType, boolean detokenizeArchived) throws ApiException, JsonProcessingException {

        batchTokenize(tokenType);

        var firstObjectId = setup.getObjectIds().get(0);
        objectsClient.deleteById(setup.getCollection().getName(), firstObjectId);

        assertIncorrectBehavior(detokenizeArchived);
    }

    @ParameterizedTest
    @MethodSource("tokenizationTypeAndArchived")
    public void cannotDetokenizeArchivedTokens(
        TokenType tokenType, boolean detokenizeArchived) throws ApiException, JsonProcessingException {

        // Batch tokenize
        TokenizeResult tokenizeResult = batchTokenize(tokenType);

        // Archive the token of the first object
        var firstObjectId = setup.getObjectIds().get(0);
        var tokenIdOfFirstObjectId = tokenizeResult.getTokenId(firstObjectId);
        tokensClient.archiveTokens(
            setup.getCollection().getName(), TokenDefinition.fromTokenIds(List.of(tokenIdOfFirstObjectId)));

        // Should succeed but now test that the incorrect result occurs
        assertIncorrectBehavior(detokenizeArchived);
    }

    @ParameterizedTest
    @MethodSource("reversibleTokenTypes")
    public void successfullyDetokenizeWithRotatedTokens(TokenType tokenType) throws ApiException, JsonProcessingException {

        // Tokenize
        TokenizeResult tokenizeResult = batchTokenize(tokenType);

        // Rotate the tokens
        Map<String, String> rotatedTokens = tokensClient.rotateTokens(
            setup.getCollection().getName(),
            tokenizeResult.getTokenIds());

        RotateResult rotateResult = new RotateResult(rotatedTokens);

        // Assert that the original token ids can no longer be used to detokenize.
        var results = singleDetokenize(tokenizeResult);
        Assertions.assertEquals(0, results.getDetokenizedTokens().size());

        // Create a new TokenizeResult in which the original token ids are replaced by the rotated ones.
        var rotatedTokenizeResult = rotateResult.rotate(tokenizeResult);

        // Detokenize now succeeds
        DetokenizeResult detokenizeResult = singleDetokenize(rotatedTokenizeResult);

        // and the rotated tokens detokenize to the expected detokenize result.
        assertDetokenizeResultIsCorrect(rotatedTokenizeResult, detokenizeResult);
    }

    @ParameterizedTest
    @EnumSource(TokenType.class)
    public void successfullyUpdateTokens(TokenType tokenType) throws ApiException {

        // Tokenize
        TokenizeResult tokenizeResult = batchTokenize(tokenType);

        // Update the tags on all tokens.
        List<String> newTags = List.of("New tag");

        UpdateTokenRequest updateTokenRequest = new UpdateTokenRequest();
        updateTokenRequest.setTags(newTags);

        tokensClient.updateTokens(
            setup.getCollection().getName(),
            TokenDefinition.fromTags(tags),
            updateTokenRequest);

        // Search tokens by the new tags.
        var queryToken = new QueryToken();
        queryToken.setTags(newTags);

        List<TokenMetadata> tokenMetadata = tokensClient.searchTokens(
            setup.getCollection().getName(), queryToken);

        SearchResult searchResult = new SearchResult(tokenMetadata);

        // and verify that all token ids are present and are associated with the correct object id.
        // Enable this assertion in the release
        assertSearchResultsMatchTokenizeResult(tokenizeResult, searchResult);
    }

    private static Stream<Arguments> tokenizationTypeAndArchived() {
        return Stream.of(
                arguments(TokenType.POINTER, true),
                arguments(TokenType.POINTER, false),
                arguments(TokenType.RANDOMIZED, true),
                arguments(TokenType.RANDOMIZED, false),
                arguments(TokenType.PCI, true),
                arguments(TokenType.PCI, false),
                arguments(TokenType.DETERMINISTIC, true),
                arguments(TokenType.DETERMINISTIC, false)
        );
    }

    private static Stream<Arguments> reversibleTokenTypes() {
        return Stream.of(
                arguments(TokenType.POINTER),
                arguments(TokenType.RANDOMIZED),
                arguments(TokenType.PCI),
                arguments(TokenType.DETERMINISTIC)
        );
    }

    // Batch tokenize the 'props' of the 'objectIds' adding the 'tags' to each token.
    private TokenizeResult batchTokenize(TokenType tokenType) throws ApiException {
        List<TokenizeRequest> tokenizeRequests = setup.getObjectIds().stream()
                .map(id -> createTokenizeRequest(tokenType, id, props, tags))
                .collect(toList());

        return new TokenizeResult(
                setup.getObjectIds(),
                tokensClient.tokenize(setup.getCollection().getName(), tokenizeRequests)
        );
    }

    private TokenizeRequest createTokenizeRequest(
        TokenType typeEnum, UUID id, List<String> props, List<String> tags) {

        TokenizeRequest request = new TokenizeRequest();
        request.setTags(tags);
        request.setType(typeEnum);
        InputObject object = new InputObject();
        object.setId(id);
        object.setFields(null);
        request.setObject(object);
        request.setProps(props);
        return request;
    }

    // singleDetokenize calls detokenize for all tokens in tokenResult, but one by one.
    private DetokenizeResult singleDetokenize(TokenizeResult tokenizeResult) throws ApiException {
        var detokenizedTokens = new ArrayList<DetokenizedToken>();

        // Detokenize each token and collect the detokenized tokens
        for (var tokenValue : tokenizeResult.getTokenValues()) {

            // Detokenize this token only
            var detokenizedToken = tokensClient.detokenize(
                setup.getCollection().getName(),
                TokenDefinition.fromTokenIds(List.of(tokenValue.getTokenId())),
                true,
                false);

            // Accumulate the detokenized token
            detokenizedTokens.addAll(detokenizedToken);
        }
        return new DetokenizeResult(detokenizedTokens);
    }

    private DetokenizeResult batchDetokenize() throws ApiException {
        // Batch detokenize by 'tags' (all tokens have the same tags, so this should detokenize all)
        var detokenizedTokens = tokensClient.detokenize(
            setup.getCollection().getName(),
            TokenDefinition.fromTags(tags),
            true,
            false);

        return new DetokenizeResult(detokenizedTokens);
    }

    // Assert that the detokenized result is correct.
    private void assertDetokenizeResultIsCorrect(
        TokenizeResult tokenizeResult,
        DetokenizeResult detokenizeResult) {

        var detokenizedTokens = detokenizeResult.getDetokenizedTokens();

        // Each detokenized token contains the tokenId that it detokenizes.
        // Assert that the list of detokenized tokens contains exactly all the tokenIds.
        assertDetokenizeReturnsExpectedTokenIds(tokenizeResult.getTokenValues(), detokenizedTokens);

        // For each detokenizedToken, compare the detokenized fields with the stored fields
        // of the object whose id is mapped to the token id.
        for (var detokenizedToken : detokenizedTokens) {
            var detokenizedFields = detokenizedToken.getFields();
            var tokenId = detokenizedToken.getTokenId();
            var objectIds = tokenizeResult.getObjectIds(tokenId);
            var firstObjectId = objectIds.get(0);
            var originalFields = setup.mapObjectIdToObjectFields.get(firstObjectId);

            Helpers.assertValuesOfKeysEqual(originalFields, detokenizedFields, props);
        }
    }

    private void assertDetokenizeReturnsExpectedTokenIds(
        List<TokenValue> tokenValues, List<DetokenizedToken> deTokenizedTokens) {

        // Get the set of tokenIds from the tokenized values
        var expectedTokenIds = tokenValues.stream().map(
            TokenValue::getTokenId).collect(Collectors.toCollection(HashSet::new));

        // Get the set of tokenIds from the detokenized tokens
        var actualTokenIds = deTokenizedTokens.stream().map(
            DetokenizedToken::getTokenId).collect(Collectors.toCollection(HashSet::new));

        Assertions.assertEquals(expectedTokenIds, actualTokenIds);
    }

    // See https://github.com/piiano/vault/issues/2047
    private void assertIncorrectBehavior(boolean detokenizeArchived) throws JsonProcessingException, ApiException {
        if (detokenizeArchived) {
            ApiMethod detokenizeMethod = () -> tokensClient.detokenize(
                setup.getCollection().getName(),
                TokenDefinition.fromTags(tags), false, true);
            ErrorHelper.expectError(detokenizeMethod, ApiError.fromStatus(Response.Status.BAD_REQUEST));
        } else {
            var result = tokensClient.detokenize(
                setup.getCollection().getName(),
                TokenDefinition.fromTags(tags), false, false);
            Assertions.assertEquals(2, result.size());
        }
    }

    private void assertSearchResultsMatchTokenizeResult(TokenizeResult tokenizeResult, SearchResult searchResult) {
        for (var tokenId : tokenizeResult.getTokenIds()) {
            Assertions.assertEquals(
                tokenizeResult.getObjectIds(tokenId),
                searchResult.getObjectIds(tokenId));
        }
    }
}
