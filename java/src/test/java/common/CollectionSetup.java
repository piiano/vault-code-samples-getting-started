package common;

import collections.CollectionsClient;
import objects.ObjectsClient;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.model.Collection;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class CollectionSetup {

    private final ApiClient apiClient = Client.create();
    private final CollectionsClient collectionsClient = new CollectionsClient(apiClient);
    private final ObjectsClient objectsClient = new ObjectsClient(apiClient);
    private final String collectionName = "customers";

    private Collection collection;
    public Map<UUID, Map<String, Object>> mapObjectIdToObjectFields;
    private ArrayList<UUID> objectIds;

    public ArrayList<UUID> getObjectIds(){
        return objectIds;
    }

    public Collection getCollection(){
        return collection;
    }

    public void setUp() throws ApiException {
        this.collection = addCollection();
        this.mapObjectIdToObjectFields = addObjects();
        this.objectIds = new ArrayList<>(mapObjectIdToObjectFields.keySet());

        assertObjectsWereInserted(mapObjectIdToObjectFields);
    }

    public void tearDown() throws ApiException {
        deleteCollectionIfExists(collectionsClient, collectionName);
    }

    private Collection addCollection() throws ApiException {

        Collection collection = Factory.createCollection(collectionName);

        deleteCollectionIfExists(collectionsClient, collection.getName());
        return collectionsClient.add(collection);
    }

    private void deleteCollectionIfExists(CollectionsClient collections, String collectionName) throws ApiException {
        try {
            collections.delete(collectionName);
        } catch (ApiException e) {
            if (e.getCode() != Response.Status.NOT_FOUND.getStatusCode()) {
                throw e;
            }
        }
    }

    // Read back the objects and verify that there were all inserted correctly.
    private void assertObjectsWereInserted(
        Map<UUID, Map<String, Object>> mapObjectIdToObjectFields) throws ApiException {

        // get all properties (props == null)
        var objects = objectsClient.get(collectionName, objectIds, null).getResults();
        for (var object : objects) {
            var objectId = UUID.fromString(object.get("id").toString());
            var fields = mapObjectIdToObjectFields.get(objectId);
            // verify all properties (props == null)
            Helpers.assertValuesOfKeysEqual(object, fields, null);
        }
    }

    private Map<UUID, Map<String, Object>> addObjects() throws ApiException {
        Map<UUID, Map<String, Object>> result = new TreeMap<>();
        var objects = Factory.createObjects();
        for (var obj : objects) {
            var id = objectsClient.add(collectionName, obj).getId();
            obj.put("id", id.toString());
            result.put(id, obj);
        }
        return result;
    }
}
