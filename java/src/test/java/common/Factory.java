package common;

import collections.CollectionsClient;
import org.openapitools.client.model.Collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Factory {

    public static Collection createCollection(String collectionName) {
        Collection collection = new Collection();
        collection.setName(collectionName);
        collection.setType(Collection.TypeEnum.PERSONS);

        collection.addPropertiesItem(
            CollectionsClient.createProp("ssn", "SSN", "Social security number",
                true, false, true, false));

        collection.addPropertiesItem(
            CollectionsClient.createProp("email", "EMAIL", "EMAIL",
                false, false, true, false));

        collection.addPropertiesItem(
            CollectionsClient.createProp("phone_number", "PHONE_NUMBER", "PHONE_NUMBER",
                false, true, true, false));

        collection.addPropertiesItem(
            CollectionsClient.createProp("zip_code_us", "ZIP_CODE_US", "ZIP_CODE_US",
                false, true, true, false));
        return collection;
    }

    public static List<Map<String, Object>> createObjects() {
        return List.of(
            createObject(
                "123-12-1234",
                "john@somemail.com",
                "+1121212123",
                "12345"),
            createObject(
                "123-12-1235",
                "mary@somemail.com",
                "+1121212124",
                "12345"),
            createObject(
                "123-12-1236",
                "eric@somemail.com",
                "+1121212125",
                "12345")
        );
    }

    private static Map<String, Object> createObject(
        String ssn, String email, String phoneNumber, String zipCodeUS) {

        Map<String, Object> objectDetails = new HashMap<>();
        objectDetails.put("ssn", ssn);
        objectDetails.put("email", email);
        objectDetails.put("phone_number", phoneNumber);
        objectDetails.put("zip_code_us", zipCodeUS);
        return objectDetails;
    }
}
