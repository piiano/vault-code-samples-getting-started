package common;

import org.openapitools.client.ApiException;

@FunctionalInterface
public interface ApiMethod {
    void call() throws ApiException;
}
