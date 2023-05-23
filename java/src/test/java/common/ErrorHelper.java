package common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.piiano.vault.client.openapi.ApiException;
import org.junit.jupiter.api.Assertions;

public class ErrorHelper {

    public static void expectError(ApiMethod method, ApiError expectedError) throws JsonProcessingException {
        try {
            method.call();
            Assertions.fail();
        } catch (ApiException ex) {
            ApiError error = ApiError.fromException(ex);
            if (expectedError.status != null) {
                Assertions.assertEquals(expectedError.status, error.status);
            }
            if (expectedError.error_code != null) {
                Assertions.assertEquals(expectedError.error_code, error.error_code);
            }
            if (expectedError.message != null) {
                Assertions.assertEquals(expectedError.message, error.message);
            }
            if (expectedError.context != null) {
                Assertions.assertEquals(expectedError.context, error.context);
            }
        }
    }
}
