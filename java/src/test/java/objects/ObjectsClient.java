package objects;

import com.piiano.vault.client.openapi.ApiClient;
import com.piiano.vault.client.openapi.ApiException;
import com.piiano.vault.client.openapi.ObjectsApi;
import com.piiano.vault.client.openapi.model.ObjectFieldsPage;
import com.piiano.vault.client.openapi.model.ObjectID;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static common.Client.*;
import static java.util.Collections.emptyList;

public class ObjectsClient {

    private final ObjectsApi objects;

    public ObjectsClient(ApiClient client) {
        objects = new ObjectsApi(client);
    }

    public ObjectID add(String collectionName, Map<String, Object> fields) throws ApiException {
        return objects.addObject(collectionName, APP_FUNCTIONALITY_REASON, fields,
                NO_ADHOC_REASON, RELOAD_CACHE, emptyList(), USE_DEFAULT_TTL, false, null);
    }

    public ObjectFieldsPage get(String collectionName, List<UUID> ids, List<String> props) throws ApiException {
        var options = new HashSet<String>();
        if (props == null) {
            options.add("unsafe");
        }
        return objects.listObjects(collectionName, APP_FUNCTIONALITY_REASON, NO_ADHOC_REASON, RELOAD_CACHE,
                null, null, false, "", emptyList(), ids, options, props);
    }

    public void deleteById(String collectionName, UUID id) throws ApiException {
        objects.deleteObjectById(collectionName, id, APP_FUNCTIONALITY_REASON,
                NO_OPTIONS, NO_ADHOC_REASON, RELOAD_CACHE, emptyList());
    }
}
