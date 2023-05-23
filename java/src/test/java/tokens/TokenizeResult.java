package tokens;

import com.piiano.vault.client.openapi.model.TokenValue;

import java.util.*;

// TokenizeResult contains all the information returned by the tokenize API method.
// - The token values.
// - Maps of token  to object ids and vice versa based on the guarantee
//   made by tokenize that the List<TokenValue> that it returns is ordered
//   by the objectIds that it is passed.
class TokenizeResult {
    private final List<TokenValue> tokenValues;
    private final Map<UUID, String> objectIdToTokenId = new TreeMap<>();
    private final Map<String, List<UUID>> tokenIdToObjectIds = new TreeMap<>();

    TokenizeResult(List<UUID> objectIds, List<TokenValue> tokenValues) {
        this.tokenValues = tokenValues;

        for (int i = 0; i < objectIds.size(); i++) {
            UUID objectId = objectIds.get(i);
            String tokenId = tokenValues.get(i).getTokenId();

            objectIdToTokenId.put(objectId, tokenId);
            var objectIdsOfTokenId = tokenIdToObjectIds.get(tokenId);
            if (objectIdsOfTokenId == null) {
                objectIdsOfTokenId = new ArrayList<UUID>();
            }
            objectIdsOfTokenId.add(objectId);
            tokenIdToObjectIds.put(tokenId, objectIdsOfTokenId);
        }
    }

    List<TokenValue> getTokenValues() {
        return tokenValues;
    }

    List<UUID> getObjectIds() {
        return new ArrayList<>(objectIdToTokenId.keySet());
    }

    List<String> getTokenIds() {
        return new ArrayList<>(tokenIdToObjectIds.keySet());
    }

    List<UUID> getObjectIds(String tokenId) {
        return tokenIdToObjectIds.get(tokenId);
    }

    String getTokenId(UUID objectId) {
        return objectIdToTokenId.get(objectId);
    }
}
