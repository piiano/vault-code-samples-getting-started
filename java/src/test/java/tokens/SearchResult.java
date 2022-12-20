package tokens;

import org.openapitools.client.model.TokenMetadata;
import org.openapitools.client.model.TokenRefMetadata;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

// SearchResult contains all the information returned by the search API method.
class SearchResult {
    private final Map<UUID, String> objectIdToTokenId = new TreeMap<>();
    private final Map<String, List<UUID>> tokenIdToObjectIds = new TreeMap<>();
    private final Map<UUID, List<TokenRefMetadata>> objectIdToRefMetadataList = new TreeMap<>();

    SearchResult(List<TokenMetadata> tokenMetadata) {
        for (var datum : tokenMetadata) {
            var tokenId = datum.getTokenId();
            var objectIds = datum.getTokens().stream().map(
                TokenRefMetadata::getObjectId
            ).collect(Collectors.toList());

            tokenIdToObjectIds.put(tokenId, objectIds);
            for (var objectId : objectIds) {
                objectIdToTokenId.put(objectId, tokenId);
                objectIdToRefMetadataList.put(objectId, datum.getTokens());
            }
        }
    }

    List<UUID> getObjectIds(String tokenId) {
        return tokenIdToObjectIds.get(tokenId);
    }

    String getTokenId(UUID objectId) {
        return objectIdToTokenId.get(objectId);
    }
}
