package collections;

import common.Client;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.CollectionsApi;
import org.openapitools.client.model.*;

import java.util.List;

public class CollectionsClient {

    private final List<String> options = Client.NO_OPTIONS;
    private final String format = "json";

    private final CollectionsApi collections;

    public CollectionsClient(ApiClient client) {
        collections = new CollectionsApi(client);
    }

    public ModelsCollection add(ModelsCollection collection) throws ApiException {
        return collections.addCollection(collection, format, options);
    }

    public void delete(String collectionName) throws ApiException {
        collections.deleteCollection(collectionName);
    }

    public static ModelsProperty createProp(
            String name, String piiTypeName, String description,
            boolean isUnique, boolean isNullable, boolean isEncrypted, boolean isIndex) {

        ModelsProperty property = new ModelsProperty();
        property.setName(name);
        property.setPiiTypeName(piiTypeName);
        property.setDescription(description);
        property.setIsUnique(isUnique);
        property.isNullable(isNullable);
        property.setIsEncrypted(isEncrypted);
        property.isIndex(isIndex);
        return property;
    }
}