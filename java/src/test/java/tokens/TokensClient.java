package tokens;


import common.Client;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.TokensApi;
import org.openapitools.client.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokensClient {

    private final List<String> options = Client.NO_OPTIONS;
    private final String reason = Client.APP_FUNCTIONALITY_REASON;
    private final String noAdhocReason = Client.NO_ADHOC_REASON;
    private final String ttl = Client.USE_DEFAULT_TTL;
    private final Boolean reloadCache = Client.reloadCache;

    private final TokensApi collections;

    public TokensClient(ApiClient client) {
        collections = new TokensApi(client);
    }

    public List<TokenValue> tokenize(String collectionName, TokenizeRequest modelsTokenizeRequest) throws ApiException {
        return collections.tokenize(collectionName, reason, modelsTokenizeRequest, ttl, noAdhocReason, reloadCache);
    }

    public List<DetokenizedToken> detokenize(String collectionName, TokenDefinition tokens, boolean includeMetadata, boolean archived) throws ApiException {
        List<String> options = new ArrayList<>();
        if (includeMetadata) {
            options.add("include_metadata");
        }
        if (archived) {
            options.add("archived");
        }
        return collections.detokenize(collectionName, reason, tokens.objectIds(), options, tokens.tags(), tokens.tokenIds(), noAdhocReason, reloadCache);
    }

    public void archiveTokens(String collectionName, TokenDefinition tokens) throws ApiException {
        collections.updateTokens(collectionName, reason, new UpdateTokenRequest(), "0", tokens.objectIds(), tokens.tags(), tokens.tokenIds(), options, noAdhocReason, reloadCache);
    }

    public List<TokenMetadata> searchTokens(String collectionName, QueryToken modelsQueryToken) throws ApiException {
        return collections.searchTokens(collectionName, reason, modelsQueryToken, options, noAdhocReason, reloadCache);
    }

    public void updateTokens(String collectionName, TokenDefinition tokens, UpdateTokenRequest modelsUpdateTokenRequest) throws ApiException {
        collections.updateTokens(collectionName, reason, modelsUpdateTokenRequest, ttl, tokens.objectIds(), tokens.tags(), tokens.tokenIds(), options, noAdhocReason, reloadCache);
    }

    public Map<String, String> rotateTokens(String collectionName, List<String> tokenIds) throws ApiException {
        return collections.rotateTokens(tokenIds, collectionName, reason, noAdhocReason, reloadCache);
    }
}