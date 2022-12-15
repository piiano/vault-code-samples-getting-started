package tokens;

import org.openapitools.client.model.DetokenizedToken;

import java.util.*;

record DetokenizeResult(List<DetokenizedToken> detokenizedTokens) {

    List<DetokenizedToken> getDetokenizedTokens() {
        return detokenizedTokens;
    }
}
