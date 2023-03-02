package collections;

import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.CollectionsApi;
import org.openapitools.client.model.*;

import static common.Client.JSON;
import static common.Client.NO_OPTIONS;

public class CollectionsClient {

    private final CollectionsApi collections;

    public CollectionsClient(ApiClient client) {
        collections = new CollectionsApi(client);
    }

    public Collection add(Collection collection) throws ApiException {
        return collections.addCollection(collection, JSON, NO_OPTIONS);
    }

    public void delete(String collectionName) throws ApiException {
        collections.deleteCollection(collectionName);
    }

    public static Property createProp(
            String name, String piiTypeName, String description,
            boolean isUnique, boolean isNullable, boolean isEncrypted, boolean isIndex) {

        Property property = new Property();
        property.setName(name);
        property.setDataTypeName(piiTypeName);
        property.setDescription(description);
        property.setIsUnique(isUnique);
        property.isNullable(isNullable);
        property.setIsEncrypted(isEncrypted);
        property.isIndex(isIndex);
        return property;
    }
}
