package tokens;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TestTokens {

    private final ApiClient apiClient = Client.create();
    private final ObjectsClient objectsClient = new ObjectsClient(apiClient);
    private final TokensClient tokensClient = new TokensClient(apiClient);
    private final CollectionSetup setup = new CollectionSetup();

    private final List<String> props = List.of("_id", "ssn", "email");
    private final List<String> tags = List.of("token_tag_1");

    @BeforeEach()
    public void beforeEach() throws ApiException {
        setup.setUp();
    }

    @AfterEach()
    public void afterEach() throws ApiException {
        setup.tearDown();
    }

    @ParameterizedTest
    @EnumSource(ModelsTokenizeRequest.TypeEnum.class)
    public void batchTokenizeAndBatchDetokenize(ModelsTokenizeRequest.TypeEnum tokenType) throws ApiException {

        TokenizeResult tokenizeResult = batchTokenize(tokenType);
        DetokenizeResult detokenizedResult = batchDetokenize();

        // Enable this assertion in the release
        // assertDetokenizeResultIsCorrect(tokenizeResult, detokenizedResult);
    }

    @ParameterizedTest
    @EnumSource(ModelsTokenizeRequest.TypeEnum.class)
    public void batchTokenizeAndSingleDetokenize(ModelsTokenizeRequest.TypeEnum tokenType) throws ApiException {

        TokenizeResult tokenizeResult = batchTokenize(tokenType);
        DetokenizeResult detokenizeResult = singleDetokenize(tokenizeResult);

        // Enable this assertion in the release
        // assertDetokenizeResultIsCorrect(tokenizeResult, detokenizeResult);
    }

    @ParameterizedTest
    @MethodSource("tokenizationTypeAndDeleted")
    public void cannotDetokenizeDeletedObjects(
        ModelsTokenizeRequest.TypeEnum tokenType, boolean detokenizeDeleted) throws ApiException, JsonProcessingException {

        batchTokenize(tokenType);

        var firstObjectId = setup.getObjectIds().get(0);
        objectsClient.deleteById(setup.getCollection().getName(), List.of(firstObjectId));

        assertIncorrectBehavior(detokenizeDeleted);
    }

    @ParameterizedTest
    @MethodSource("tokenizationTypeAndDeleted")
    public void cannotDetokenizeDeletedTokens(
        ModelsTokenizeRequest.TypeEnum tokenType, boolean detokenizeDeleted) throws ApiException, JsonProcessingException {

        // Batch tokenize
        TokenizeResult tokenizeResult = batchTokenize(tokenType);

        // Delete the token of the first object
        var firstObjectId = setup.getObjectIds().get(0);
        var tokenIdOfFirstObjectId = tokenizeResult.getTokenId(firstObjectId);
        tokensClient.deleteTokens(
            setup.getCollection().getName(), TokenDefinition.fromTokenIds(List.of(tokenIdOfFirstObjectId)));

        // Should succeed but now test that the incorrect result occurs
        assertIncorrectBehavior(detokenizeDeleted);
    }

    @ParameterizedTest
    @EnumSource(ModelsTokenizeRequest.TypeEnum.class)
    public void successfullyDetokenizeWithRotatedTokens(ModelsTokenizeRequest.TypeEnum tokenType) throws ApiException, JsonProcessingException {

        // Tokenize
        TokenizeResult tokenizeResult = batchTokenize(tokenType);

        // Rotate the tokens
        List<ModelsRotatedToken> rotatedTokens = tokensClient.rotateTokens(
            setup.getCollection().getName(),
            tokenizeResult.getTokenIds());

        RotateResult rotateResult = new RotateResult(rotatedTokens);

        // Assert that the original token ids can no longer be used to detokenize.
        ErrorHelper.expectError(
            () -> singleDetokenize(tokenizeResult),
            ApiError.fromStatusCodeAndMessage(Response.Status.NOT_FOUND,
                "PV3087",
                "One or more token IDs not found."));

        // Create a new TokenizeResult in which the original token ids are replaced by the rotated ones.
        var rotatedTokenizeResult = rotateResult.rotate(tokenizeResult);

        // Detokenize now succeeds
        DetokenizeResult detokenizeResult = singleDetokenize(rotatedTokenizeResult);

        // This fails: see https://github.com/piiano/vault/issues/1849
        // assertDetokenizeResultIsCorrect(rotatedTokenizeResult, detokenizeResult);
    }

    @ParameterizedTest
    @EnumSource(ModelsTokenizeRequest.TypeEnum.class)
    public void successfullyUpdateTokens(ModelsTokenizeRequest.TypeEnum tokenType) throws ApiException {

        // Tokenize
        TokenizeResult tokenizeResult = batchTokenize(tokenType);

        // Update the tags on all tokens.
        List<String> newTags = List.of("New tag");

        ModelsUpdateTokenRequest updateTokenRequest = new ModelsUpdateTokenRequest();
        updateTokenRequest.setTags(newTags);

        tokensClient.updateTokens(
            setup.getCollection().getName(),
            TokenDefinition.fromTags(tags),
            updateTokenRequest);

        // Search tokens by the new tags.
        var queryToken = new ModelsQueryToken();
        queryToken.setTag(newTags);

        List<ModelsTokenMetadata> tokenMetadata = tokensClient.searchTokens(
            setup.getCollection().getName(), queryToken);

        SearchResult searchResult = new SearchResult(tokenMetadata);

        // and verify that all token ids are present and are associated with the correct object id.
        // Enable this assertion in the release
        // assertSearchResultsMatchTokenizeResult(tokenizeResult, searchResult);
    }

    private static Stream<Arguments> tokenizationTypeAndDeleted() {
        return Stream.of(
            arguments(ModelsTokenizeRequest.TypeEnum.POINTER, true),
            arguments(ModelsTokenizeRequest.TypeEnum.POINTER, false),
            arguments(ModelsTokenizeRequest.TypeEnum.VALUE, true),
            arguments(ModelsTokenizeRequest.TypeEnum.VALUE, false)
        );
    }

    // Batch tokenize the 'props' of the 'objectIds' adding the 'tags' to each token.
    private TokenizeResult batchTokenize(ModelsTokenizeRequest.TypeEnum tokenType) throws ApiException {
        return new TokenizeResult(
            setup.getObjectIds(),
            tokensClient.tokenize(
                setup.getCollection().getName(),
                createTokenizeRequest(tokenType, setup.getObjectIds(), props, tags)));
    }

    private ModelsTokenizeRequest createTokenizeRequest(
        ModelsTokenizeRequest.TypeEnum typeEnum,
        List<UUID> ids, List<String> props, List<String> tags) {
        ModelsTokenizeRequest request = new ModelsTokenizeRequest();
        request.setTags(tags);
        request.setType(typeEnum);
        request.setReuseTokenId(false);
        request.setReversible(true);
        request.setObjectIds(ids);
        request.setProps(props);
        return request;
    }

    // singleDetokenize calls detokenize for all tokens in tokenResult, but one by one.
    private DetokenizeResult singleDetokenize(TokenizeResult tokenizeResult) throws ApiException {
        var detokenizedTokens = new ArrayList<ModelsDetokenizedToken>();

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
            var objectId = tokenizeResult.getObjectId(tokenId);
            var originalFields = setup.mapObjectIdToObjectFields.get(objectId);

            Helpers.assertValuesOfKeysEqual(originalFields, detokenizedFields, props);
        }
    }

    private void assertDetokenizeReturnsExpectedTokenIds(
        List<ModelsTokenValue> tokenValues, List<ModelsDetokenizedToken> deTokenizedTokens) {

        // Get the set of tokenIds from the tokenized values
        var expectedTokenIds = tokenValues.stream().map(
            ModelsTokenValue::getTokenId).collect(Collectors.toCollection(HashSet::new));

        // Get the set of tokenIds from the detokenized tokens
        var actualTokenIds = deTokenizedTokens.stream().map(
            ModelsDetokenizedToken::getTokenId).collect(Collectors.toCollection(HashSet::new));

        Assertions.assertEquals(expectedTokenIds, actualTokenIds);
    }

    // See https://github.com/piiano/vault/issues/1847
    private void assertIncorrectBehavior(boolean detokenizeDeleted) throws JsonProcessingException, ApiException {
        if (detokenizeDeleted) {
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
        for(var tokenId : tokenizeResult.getTokenIds()) {
            var objectIds = searchResult.getObjectIds(tokenId);
            Assertions.assertEquals(1, objectIds.size());
            Assertions.assertEquals(tokenizeResult.getObjectId(tokenId), objectIds.get(0));
        }
    }
}
