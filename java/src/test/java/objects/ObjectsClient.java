package objects;

import common.Client;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.CollectionsApi;
import org.openapitools.client.api.ObjectsApi;
import org.openapitools.client.model.ModelsCollection;
import org.openapitools.client.model.ModelsObjectFieldsPage;
import org.openapitools.client.model.ModelsObjectID;

import java.util.*;

public class ObjectsClient {

    private final String reason = Client.APP_FUNCTIONALITY_REASON;
    private final String noAdhocReason = Client.NO_ADHOC_REASON;
    private final String ttl = Client.USE_DEFAULT_TTL;
    private final Boolean reloadCache = Client.reloadCache;

    private final ObjectsApi objects;

    public ObjectsClient(ApiClient client) {
        objects = new ObjectsApi(client);
    }

    public ModelsObjectID add(String collectionName, Map<String, Object> fields) throws ApiException {
        return objects.addObject(collectionName, reason, fields, noAdhocReason, reloadCache, ttl);
    }

    public ModelsObjectFieldsPage get(String collectionName, List<UUID> ids, List<String> props) throws ApiException {
        var options = new ArrayList<String>();
        if (props == null) {
            options.add("unsafe");
        }
        return objects.getObjects(collectionName, reason, noAdhocReason, reloadCache, null, null, ids, options, props);
    }

    public void deleteById(String collectionName, List<UUID> ids) throws ApiException {
        objects.deleteObjectById(collectionName, ids, reason, Client.NO_OPTIONS, noAdhocReason, reloadCache);
    }
}