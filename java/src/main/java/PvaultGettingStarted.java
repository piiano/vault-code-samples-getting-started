import com.google.common.collect.ImmutableList;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.api.*;
import org.openapitools.client.model.*;

import java.util.*;

import static java.util.Collections.emptyList;

public class PvaultGettingStarted {

    public static final String HEALTH_PASS = "pass";
    public static final String JSON = "json";
    public static final String CUSTOMERS_COLLECTION = "customers";
    public static final String APP_FUNCTIONALITY_REASON = "AppFunctionality";
    public static final List<String> NO_OPTIONS = emptyList();
    public static final String NO_ADHOC_REASON = "";
    public static final String USE_DEFAULT_TTL = "";
    public static final Integer DEFAULT_PAGE_SIZE = 100;
    public static final String ALL_UNSAFE = "unsafe"; // fetch all the properties

    public static final int DEFAULT_PVAULT_PORT = 8123;

    public void run() throws Exception {
        ApiClient pvaultClient = getApiClient();

        print("\n\n== Steps 1 + 2: Connect to Piiano vault and check status ==\n\n");
        checkPvaultStatus(pvaultClient);

        print("\n\n== Step 3: Create a collection ==\n\n");
        createCollection(pvaultClient);

        print("\n\n== Step 4: Add data ==\n\n");
        ObjectsApi objectsApi = new ObjectsApi(pvaultClient);
        UUID customer1ID = addData(objectsApi);

        print("\n\n== Step 5: Tokenize data ==\n\n");
        TokensApi tokensApi = new TokensApi(pvaultClient);
        String token = tokenizeData(customer1ID, tokensApi);

        detokenizeToken(tokensApi, token);

        print("\n\n== Step 6: Query your data ==\n\n");
        searchObjects(objectsApi);
        getObjectsById(objectsApi, customer1ID);
        getTransformedPropertiesOfObjects(objectsApi, customer1ID);

        print("\n\n== Step 7: Delete data ==\n\n");
        deleteToken(tokensApi, token);
        deleteObject(objectsApi, customer1ID);

        print("Done!\n");
    }

    private static void checkPvaultStatus(ApiClient pvaultClient) throws ApiException {

        SystemApi systemApi = new SystemApi(pvaultClient);
        String controlStatus = systemApi.controlHealth().getStatus();
        String dataStatus = systemApi.dataHealth().getStatus();

        print("control status: ", controlStatus);
        print("data status: ", dataStatus);
        assert HEALTH_PASS.equals(controlStatus) && HEALTH_PASS.equals(dataStatus);
    }

    private static void createCollection(ApiClient pvaultClient) throws ApiException {
        // Note: Adding a collection with pvschema is not supported in the SDK
        // Throughout this code we will use JSON exclusively.
        CollectionsApi collectionApi = new CollectionsApi(pvaultClient);

        ModelsCollection collection = new ModelsCollection();
        collection.setName(CUSTOMERS_COLLECTION);
        collection.setType(ModelsCollection.TypeEnum.PERSONS);

        collection.addPropertiesItem(
                buildModelsProperty("ssn", "SSN", "Social security number",
                true, false, true, false));

        collection.addPropertiesItem(
                buildModelsProperty("email", "EMAIL", "EMAIL",
                        false, false, true, false));

        collection.addPropertiesItem(
                buildModelsProperty("phone_number", "PHONE_NUMBER", "PHONE_NUMBER",
                        false, true, true, false));

        collection.addPropertiesItem(
                buildModelsProperty("zip_code_us", "ZIP_CODE_US", "ZIP_CODE_US",
                        false, true, true, false));

        collectionApi.addCollection(collection, JSON, NO_OPTIONS);

        // Check the collections has been added.
        collection = collectionApi.getCollection(CUSTOMERS_COLLECTION, JSON, NO_OPTIONS);
        assert collection != null;
        print("collection: ", collection.toString());
    }

    private static UUID addData(ObjectsApi objectsApi) throws ApiException {

        Map<String, Object> objectDetails = buildObjectDetails(
                "123-12-1234", "john@somemail.com",
                "+1-121212123", "12345");
        UUID customer1ID = objectsApi.addObject(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, objectDetails,
                NO_ADHOC_REASON, false, USE_DEFAULT_TTL).getId();
        print("customer1 ID: ", customer1ID.toString());

        objectDetails = buildObjectDetails(
                "123-12-1235", "mary@somemail.com",
                "+1-121212124", "12345");
        UUID customer2ID = objectsApi.addObject(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, objectDetails,
                NO_ADHOC_REASON, false, USE_DEFAULT_TTL).getId();
        print("customer2 ID: ", customer2ID.toString());

        objectDetails = buildObjectDetails(
                "123-12-1236", "eric@somemail.com",
                "+1-121212125", "12345");

        UUID customer3ID = objectsApi.addObject(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, objectDetails,
                NO_ADHOC_REASON, false, USE_DEFAULT_TTL).getId();
        print("customer3 ID: ", customer3ID.toString());

        return customer1ID;
    }

    private static String tokenizeData(UUID id, TokensApi tokensApi) throws ApiException {

        ModelsTokenizeRequest tokenizeRequest = new ModelsTokenizeRequest();
        tokenizeRequest.addObjectIdsItem(id);
        tokenizeRequest.addPropsItem("email");
        tokenizeRequest.setType(ModelsTokenizeRequest.TypeEnum.POINTER);
        tokenizeRequest.setTags(ImmutableList.of("token_tag"));

        String token = tokensApi.tokenize(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, tokenizeRequest,
                USE_DEFAULT_TTL, NO_ADHOC_REASON, false).get(0).getTokenId();
        print("Token: ", token);
        return token;
    }

    private static void detokenizeToken(TokensApi tokensApi, String token) throws ApiException {

        String returnedEmail = tokensApi.detokenize(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, emptyList(),
                        NO_OPTIONS, emptyList(), ImmutableList.of(token), NO_ADHOC_REASON, false)
                .get(0).getFields().get("email").toString();
        assert "john@somemail.com".equals(returnedEmail);
    }

    private static void searchObjects(ObjectsApi objectsApi) throws ApiException {

        ModelsQuery query = new ModelsQuery();
        query.setMatch(Collections.singletonMap("email", "john@somemail.com"));
        ModelsObjectFieldsPage objectIdsPage =
                objectsApi.searchObjects(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, query, NO_ADHOC_REASON,
                        false, DEFAULT_PAGE_SIZE, "", ImmutableList.of(ALL_UNSAFE), null);

        assert 1 == objectIdsPage.getResults().size();
        Map<String, Object> searchResult = objectIdsPage.getResults().get(0);
        assert "john@somemail.com".equals(searchResult.get("email"));
        assert "123-12-1234".equals(searchResult.get("ssn"));
        assert "+1-121212123".equals(searchResult.get("phone_number"));
        assert "12345".equals(searchResult.get("zip_code_us"));
        print("object retrieved by search: ", searchResult.toString());
    }

    private static void getObjectsById(ObjectsApi objectsApi, UUID id) throws ApiException {

        ModelsObjectFieldsPage objectIdsPage = objectsApi.getObjects(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON,
                NO_ADHOC_REASON, false, null, "", ImmutableList.of(id),
                emptyList(), ImmutableList.of("ssn"));

        assert 1 == objectIdsPage.getResults().size();
        Map<String, Object> searchResult = objectIdsPage.getResults().get(0);
        assert 1 == searchResult.size();
        assert "123-12-1234".equals(searchResult.get("ssn"));
        print("object retrieved by id: ", searchResult.toString());
    }

    private static void getTransformedPropertiesOfObjects(ObjectsApi objectsApi, UUID id) throws ApiException {

        ModelsObjectFieldsPage objectIdsPage = objectsApi.getObjects(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON,
                NO_ADHOC_REASON, false, null, "", ImmutableList.of(id),
                emptyList(), ImmutableList.of("ssn.mask", "email.mask", "phone_number.mask"));

        assert 1 == objectIdsPage.getResults().size();
        Map<String, Object> searchResult = objectIdsPage.getResults().get(0);
        assert "j***@somemail.com".equals(searchResult.get("email"));
        assert "***-**-1234".equals(searchResult.get("ssn"));
        assert "********2123".equals(searchResult.get("phone_number"));
        print("transformed propertied retrieved: ", searchResult.toString());
    }

    private static void deleteToken(TokensApi tokensApi, String token) throws ApiException {

        tokensApi.deleteTokens(CUSTOMERS_COLLECTION, APP_FUNCTIONALITY_REASON, emptyList(), emptyList(),
                ImmutableList.of(token), NO_OPTIONS, NO_ADHOC_REASON, false);
    }

    private static void deleteObject(ObjectsApi objectsApi, UUID id) throws ApiException {

        objectsApi.deleteObjectById(CUSTOMERS_COLLECTION, ImmutableList.of(id), APP_FUNCTIONALITY_REASON,
                NO_OPTIONS, NO_ADHOC_REASON, false);
    }

    private static ApiClient getApiClient() {

        // Create configuration, bearer auth and client API
        ApiClient pvaultClient = Configuration.getDefaultApiClient();
        pvaultClient.setBasePath("http://localhost:" + DEFAULT_PVAULT_PORT);
        pvaultClient.setBearerToken("pvaultauth");
        pvaultClient.addDefaultHeader("Content-Type", "application/json");
        return pvaultClient;
    }

    private static ModelsProperty buildModelsProperty(
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

    private static Map<String, Object> buildObjectDetails(
            String ssn, String email, String phoneNumber, String zipCodeUS) {

        Map<String, Object> objectDetails = new HashMap<>();
        objectDetails.put("ssn", ssn);
        objectDetails.put("email", email);
        objectDetails.put("phone_number", phoneNumber);
        objectDetails.put("zip_code_us", zipCodeUS);
        return objectDetails;
    }

    private static void print(String... args) {
        for (String arg : args) {
            System.out.print(arg);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        PvaultGettingStarted pvaultGettingStarted = new PvaultGettingStarted();
        try {
            pvaultGettingStarted.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
