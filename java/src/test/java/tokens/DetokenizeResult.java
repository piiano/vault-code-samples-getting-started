package tokens;

import org.openapitools.client.model.ModelsDetokenizedToken;

import java.util.*;

record DetokenizeResult(List<ModelsDetokenizedToken> detokenizedTokens) {

    List<ModelsDetokenizedToken> getDetokenizedTokens() {
        return detokenizedTokens;
    }
}
