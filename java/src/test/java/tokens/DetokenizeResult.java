package tokens;

import com.piiano.vault.client.openapi.model.DetokenizedToken;

import java.util.List;

record DetokenizeResult(List<DetokenizedToken> detokenizedTokens) {

    List<DetokenizedToken> getDetokenizedTokens() {
        return detokenizedTokens;
    }
}
