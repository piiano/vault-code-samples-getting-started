package tokens;

import org.openapitools.client.model.ModelsRotatedToken;
import org.openapitools.client.model.ModelsTokenValue;

import java.util.*;
import java.util.stream.Collectors;

// RotateResult contains all the information returned by the rotate API method.
// The rotate method converts an existing TokenizeResult with a new one replacing
// the old tokenIds with the rotated ones.
class RotateResult {
    private final Map<String, String> oldTokenIdToNewTokenId = new TreeMap<>();

    RotateResult(List<ModelsRotatedToken> rotatedTokens) {
        for (var rotatedToken : rotatedTokens) {
            String newTokenId = rotatedToken.getNewTokenId();
            String oldTokenId = rotatedToken.getOldTokenId();
            oldTokenIdToNewTokenId.put(oldTokenId, newTokenId);
        }
    }

    String getNewTokenId(String oldTokenId) {
        return oldTokenIdToNewTokenId.get(oldTokenId);
    }

    public TokenizeResult rotate(TokenizeResult tokenizeResult) {
        var objectIds = tokenizeResult.getObjectIds();
        var newTokenValues = objectIds.stream().map(
            objectId -> {
                var oldTokenId = tokenizeResult.getTokenId(objectId);
                var newTokenId = oldTokenIdToNewTokenId.get(oldTokenId);
                return toTokenValue(newTokenId);
            }
        ).collect(Collectors.toList());
        return new TokenizeResult(
            objectIds,              // use original object objectIds
            newTokenValues);
    }

    ModelsTokenValue toTokenValue(String tokenId) {
        var newTokenValue = new ModelsTokenValue();
        newTokenValue.setTokenId(tokenId);
        return newTokenValue;
    }
}
