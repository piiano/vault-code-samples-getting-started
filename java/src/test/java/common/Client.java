package common;

import org.openapitools.client.ApiClient;
import org.openapitools.client.Configuration;

import java.util.List;

import static java.util.Collections.emptyList;

public class Client {

    public static final int DEFAULT_PVAULT_PORT = 8123;
    public static final String NO_ADHOC_REASON = "";
    public static final String APP_FUNCTIONALITY_REASON = "AppFunctionality";
    public static final String USE_DEFAULT_TTL = "";
    public static final String TRANSACTION_ID=""; // Transaction ID is only relevant for advanced usage
    public static final Boolean RELOAD_CACHE = false;
    public static final String JSON = "json";
    public static final List<String> NO_OPTIONS = emptyList();

    public static ApiClient create() {

        // Create configuration, bearer auth and client API
        ApiClient apiClient = Configuration.getDefaultApiClient();
        apiClient.setBasePath("http://localhost:" + DEFAULT_PVAULT_PORT);
        apiClient.setBearerToken("pvaultauth");
        apiClient.addDefaultHeader("Content-Type", "application/json");

        return apiClient;
    }
}
