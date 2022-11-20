package tokens;


import org.openapitools.client.model.ModelsTokenValue;

import java.util.*;

// TokenizeResult contains all the information returned by the tokenize API method.
// - The token values.
// - Maps of token  to object ids and vice versa based on the guarantee
//   made by tokenize that the List<ModelsTokenValue> that it returns is ordered
//   by the objectIds that it is passed.
class TokenizeResult {
    private final List<ModelsTokenValue> tokenValues;
    private final Map<UUID, String> objectIdToTokenId = new TreeMap<>();
    private final Map<String, List<UUID>> tokenIdToObjectIds = new TreeMap<>();

    TokenizeResult(List<UUID> objectIds, List<ModelsTokenValue> tokenValues) {
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

    List<ModelsTokenValue> getTokenValues() {
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
