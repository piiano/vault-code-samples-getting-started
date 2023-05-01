package objects;

import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ObjectsApi;
import org.openapitools.client.model.ObjectFieldsPage;
import org.openapitools.client.model.ObjectID;

import java.util.*;

import static common.Client.*;

public class ObjectsClient {

    private final ObjectsApi objects;

    public ObjectsClient(ApiClient client) {
        objects = new ObjectsApi(client);
    }

    public ObjectID add(String collectionName, Map<String, Object> fields) throws ApiException {
        return objects.addObject(collectionName, APP_FUNCTIONALITY_REASON, fields,
                NO_ADHOC_REASON, RELOAD_CACHE, USE_DEFAULT_TTL);
    }

    public ObjectFieldsPage get(String collectionName, List<UUID> ids, List<String> props) throws ApiException {
        var options = new HashSet<String>();
        if (props == null) {
            options.add("unsafe");
        }
        return objects.listObjects(collectionName, APP_FUNCTIONALITY_REASON, NO_ADHOC_REASON,
                RELOAD_CACHE, null, null, "", ids, options, props);
    }

    public void deleteById(String collectionName, UUID id) throws ApiException {
        objects.deleteObjectById(collectionName, id, APP_FUNCTIONALITY_REASON,
                NO_OPTIONS, NO_ADHOC_REASON, RELOAD_CACHE);
    }
}
