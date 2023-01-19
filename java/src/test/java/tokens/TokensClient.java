package tokens;

import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.TokensApi;
import org.openapitools.client.model.*;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static common.Client.*;

public class TokensClient {

    private final TokensApi collections;

    public TokensClient(ApiClient client) {
        collections = new TokensApi(client);
    }

    public List<TokenValue> tokenize(String collectionName, List<TokenizeRequest> tokenizeRequest) throws ApiException {
        return collections.tokenize(collectionName, APP_FUNCTIONALITY_REASON,
                tokenizeRequest, USE_DEFAULT_TTL, NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public List<DetokenizedToken> detokenize(String collectionName, TokenDefinition tokens, boolean includeMetadata, boolean archived) throws ApiException {
        List<String> options = new ArrayList<>();
        if (includeMetadata) {
            options.add("include_metadata");
        }
        if (archived) {
            options.add("archived");
        }
        return collections.detokenize(collectionName, APP_FUNCTIONALITY_REASON, tokens.objectIds(), options,
                tokens.tags(), tokens.tokenIds(), NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public void archiveTokens(String collectionName, TokenDefinition tokens) throws ApiException {
        collections.updateTokens(collectionName, APP_FUNCTIONALITY_REASON, new UpdateTokenRequest(), "0",
                tokens.objectIds(), tokens.tags(), tokens.tokenIds(), NO_OPTIONS, NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public List<TokenMetadata> searchTokens(String collectionName, QueryToken queryToken) throws ApiException {
        return collections.searchTokens(collectionName, APP_FUNCTIONALITY_REASON, queryToken,
                NO_OPTIONS, NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public void updateTokens(String collectionName, TokenDefinition tokens, UpdateTokenRequest updateTokenRequest) throws ApiException {
        collections.updateTokens(collectionName, APP_FUNCTIONALITY_REASON, updateTokenRequest, USE_DEFAULT_TTL,
                tokens.objectIds(), tokens.tags(), tokens.tokenIds(), NO_OPTIONS, NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public Map<String, String> rotateTokens(String collectionName, List<String> tokenIds) throws ApiException {
        return collections.rotateTokens(tokenIds, collectionName, APP_FUNCTIONALITY_REASON,
                NO_ADHOC_REASON, RELOAD_CACHE);
    }
}
