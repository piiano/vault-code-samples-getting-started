package tokens;

import org.openapitools.client.model.DetokenizedToken;

import java.util.List;

record DetokenizeResult(List<DetokenizedToken> detokenizedTokens) {

    List<DetokenizedToken> getDetokenizedTokens() {
        return detokenizedTokens;
    }
}
