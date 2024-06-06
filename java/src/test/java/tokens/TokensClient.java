package tokens;

import com.piiano.vault.client.openapi.ApiClient;
import com.piiano.vault.client.openapi.ApiException;
import com.piiano.vault.client.openapi.TokensApi;
import com.piiano.vault.client.openapi.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static common.Client.*;
import static java.util.Collections.emptyList;

public class TokensClient {

    private final TokensApi collections;

    public TokensClient(ApiClient client) {
        collections = new TokensApi(client);
    }

    public List<TokenValue> tokenize(String collectionName, List<TokenizeRequest> tokenizeRequest) throws ApiException {
        return collections.tokenize(collectionName, APP_FUNCTIONALITY_REASON, tokenizeRequest,
                USE_DEFAULT_TTL, emptyList(), NO_TRANSACTION_ID, NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public List<DetokenizedToken> detokenize(String collectionName, TokenDefinition tokens, boolean includeMetadata, boolean archived) throws ApiException {
        Set<String> options = new HashSet<>();
        if (includeMetadata) {
            options.add("include_metadata");
        }
        if (archived) {
            options.add("archived");
        }
        return collections.detokenize(collectionName, APP_FUNCTIONALITY_REASON, tokens.objectIds(),
                tokens.tags(), tokens.tokenIds(), emptyList(), options, emptyList(), NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public void archiveTokens(String collectionName, TokenDefinition tokens) throws ApiException {
        collections.updateTokens(collectionName, APP_FUNCTIONALITY_REASON, new UpdateTokenRequest(), "0",
                tokens.objectIds(), tokens.tags(), tokens.tokenIds(), NO_OPTIONS, emptyList(), NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public List<TokenMetadata> searchTokens(String collectionName, QueryToken queryToken) throws ApiException {
        return collections.searchTokens(collectionName, APP_FUNCTIONALITY_REASON, queryToken,
                NO_OPTIONS, emptyList(), NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public void updateTokens(String collectionName, TokenDefinition tokens, UpdateTokenRequest updateTokenRequest) throws ApiException {
        collections.updateTokens(collectionName, APP_FUNCTIONALITY_REASON, updateTokenRequest, USE_DEFAULT_TTL,
                tokens.objectIds(), tokens.tags(), tokens.tokenIds(), NO_OPTIONS, emptyList(), NO_ADHOC_REASON, RELOAD_CACHE);
    }

    public Map<String, String> rotateTokens(String collectionName, List<String> tokenIds) throws ApiException {
        return collections.rotateTokens(tokenIds, collectionName, APP_FUNCTIONALITY_REASON,
                emptyList(), NO_ADHOC_REASON, RELOAD_CACHE);
    }
}
