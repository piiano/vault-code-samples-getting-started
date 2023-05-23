package common;

import com.piiano.vault.client.openapi.ApiException;

@FunctionalInterface
public interface ApiMethod {
    void call() throws ApiException;
}
