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
        return new TokenizeResult(
            tokenizeResult.getObjectIds(),              // use original object ids
            tokenizeResult.getTokenIds().stream().map(  // with new token ids
                tokenId -> toTokenValue(getNewTokenId(tokenId))
            ).collect(Collectors.toList()));
    }

    ModelsTokenValue toTokenValue(String tokenId) {
        var newTokenValue = new ModelsTokenValue();
        newTokenValue.setTokenId(tokenId);
        return newTokenValue;
    }
}
