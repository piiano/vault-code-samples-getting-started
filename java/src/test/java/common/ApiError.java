package common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piiano.vault.client.openapi.ApiException;

import javax.ws.rs.core.Response;
import java.util.HashMap;

public class ApiError {
    public Response.Status status;
    public String error_code;
    public String error_url;
    public String message;
    public HashMap<String, String> context;

    public ApiError() {
    }

    private ApiError(Response.Status status, String error_code, String error_url, String message, HashMap<String, String> context) {
        this.status = status;
        this.error_code = error_code;
        this.error_url = error_url;
        this.message = message;
        this.context = context;
    }

    public static ApiError fromException(ApiException ex) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var error = mapper.readValue(ex.getResponseBody(), ApiError.class);
        error.status = Response.Status.fromStatusCode(ex.getCode());
        return error;
    }

    public static ApiError fromStatus(Response.Status status) {
        return new ApiError(status, null, null, null, null);
    }

    public static ApiError fromStatusCodeAndMessage(Response.Status status, String error_code, String error_url, String message) {
        return new ApiError(status, error_code, error_url, message, null);
    }
}
