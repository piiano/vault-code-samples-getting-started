import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.piiano.vault.client.openapi.*;
import com.piiano.vault.client.openapi.model.Collection;
import com.piiano.vault.client.openapi.model.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class PvaultGettingStarted {

    public static final String COLLECTION_NAME = "customers";
    public static final String HEALTH_PASS = "pass";
    public static final String JSON = "json";
    public static final String APP_FUNCTIONALITY_REASON = "AppFunctionality";
    public static final String NO_ADHOC_REASON = null;
    public static final Set<String> NO_OPTIONS = emptySet();
    public static final String USE_DEFAULT_TTL = "";
    public static final String UNSAFE_OPTION = "unsafe"; // fetch all the properties
    public static final String NO_TRANSACTION_ID = null; // Transaction ID is only relevant for advanced usage
    public static final int PVAULT_ADDRESS = 8123;

    public void run() throws Exception {

        print("\n\n== Steps 1 + 2: Connect to Piiano vault and check status ==\n\n");
        ApiClient pvaultClient = getApiClient();
        checkPvaultStatus(pvaultClient);

        print("\n\n== Step 3: Create a collection ==\n\n");
        CollectionsApi collectionsApi = new CollectionsApi(pvaultClient);
        CollectionPropertiesApi collectionpropapi = new CollectionPropertiesApi(pvaultClient);
        // deleteCollection(collectionsApi);
        verifyNoCollections(collectionpropapi);
        createCollection(collectionsApi);

        print("\n\n== Step 4: Add data ==\n\n");
        ObjectsApi objectsApi = new ObjectsApi(pvaultClient);
        List<UUID> customerIds = addData(objectsApi);

        print("\n\n== Step 5: Tokenize data ==\n\n");
        TokensApi tokensApi = new TokensApi(pvaultClient);
        String token = tokenizeData(customerIds.get(0), tokensApi);

        detokenizeToken(tokensApi, token);

        print("\n\n== Step 6: Query your data ==\n\n");
        queryAllObjectsWithPageSize(objectsApi);
        queryPropertiesOfObjectsById(objectsApi, customerIds.get(0));
        getTransformedPropertiesOfObjects(objectsApi, customerIds.get(0));

        print("\n\n== Step 7: Delete data ==\n\n");
        deleteToken(tokensApi, token);
        deleteObject(objectsApi, customerIds.get(0));

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

    private static void deleteCollection(CollectionsApi collectionApi) {
        print("Clearing the collection from previous runs");
        try {
            collectionApi.deleteCollection(COLLECTION_NAME);
        } catch (ApiException e) {
            // test collection wasn't exist, continue test..
        }
    }

    // for safety reasons refuse to run if the collection already exists
    private static void verifyNoCollections(CollectionPropertiesApi collectionpropApi) throws ApiException {
        print("Verifying the test collection is not present");
        try {
            List<Property> mp =
                    collectionpropApi.listCollectionProperties(COLLECTION_NAME, NO_OPTIONS);
            throw new RuntimeException("Collection " + COLLECTION_NAME + " already exists.\n" +
                    "Recreate the Vault from scratch or uncomment deleteCollection()" +
                    " in this code. Bailing out.\n");
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                print("Collection " + COLLECTION_NAME + " not found. Will create it");
            } else {
                throw new ApiException(e);
            }
        }
    }

    private static void createCollection(CollectionsApi collectionApi) throws ApiException {
        // Note: Adding a collection with pvschema is not supported in the SDK
        // Throughout this code we will use JSON exclusively.

        Collection collection = new Collection();
        collection.setName(COLLECTION_NAME);
        collection.setType(Collection.TypeEnum.PERSONS);

        collection.addPropertiesItem(
                buildProperty("ssn", "SSN", "Social security number",
                        true, false, true, false));

        collection.addPropertiesItem(
                buildProperty("email", "EMAIL", "EMAIL",
                        false, false, true, false));

        collection.addPropertiesItem(
                buildProperty("phone_number", "PHONE_NUMBER", "PHONE_NUMBER",
                        false, true, true, false));

        collection.addPropertiesItem(
                buildProperty("zip_code_us", "ZIP_CODE_US", "ZIP_CODE_US",
                        false, true, true, false));

        collectionApi.addCollection(collection, JSON, NO_OPTIONS);

        // Check the collections has been added.
        collection = collectionApi.getCollection(COLLECTION_NAME, JSON, NO_OPTIONS);
        assert collection != null;
        print("collection: ", collection.toString());
    }

    private static List<UUID> addData(ObjectsApi objectsApi) throws ApiException {

        List<UUID> customerIds = new ArrayList<>();

        Map<String, Object> objectDetails = buildObjectDetails(
                "123-12-1234", "john@somemail.com",
                "+1-121212123", "12345");
        UUID customer1ID = objectsApi.addObject(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, objectDetails,
                NO_ADHOC_REASON, false, USE_DEFAULT_TTL).getId();
        customerIds.add(customer1ID);
        print("customer1 ID: ", customer1ID.toString());

        objectDetails = buildObjectDetails(
                "123-12-1235", "mary@somemail.com",
                "+1-121212124", "12345");
        UUID customer2ID = objectsApi.addObject(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, objectDetails,
                NO_ADHOC_REASON, false, USE_DEFAULT_TTL).getId();
        customerIds.add(customer2ID);
        print("customer2 ID: ", customer2ID.toString());

        objectDetails = buildObjectDetails(
                "123-12-1236", "eric@somemail.com",
                "+1-121212125", "12345");

        UUID customer3ID = objectsApi.addObject(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, objectDetails,
                NO_ADHOC_REASON, false, USE_DEFAULT_TTL).getId();
        customerIds.add(customer3ID);
        print("customer3 ID: ", customer3ID.toString());

        return customerIds;
    }

    private static String tokenizeData(UUID id, TokensApi tokensApi) throws ApiException {

        TokenizeRequest tokenizeRequest = new TokenizeRequest();
        InputObject object = new InputObject();
        object.setId(id);
        object.setFields(null);
        tokenizeRequest.setObject(object);
        tokenizeRequest.addPropsItem("email");
        tokenizeRequest.setType(TokenType.POINTER);
        tokenizeRequest.setTags(ImmutableList.of("token_tag"));

        String token = tokensApi.tokenize(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, List.of(tokenizeRequest),
                USE_DEFAULT_TTL, NO_TRANSACTION_ID, NO_ADHOC_REASON, false).get(0).getTokenId();
        print("Token: ", token);
        return token;
    }

    private static void detokenizeToken(TokensApi tokensApi, String token) throws ApiException {

        String returnedEmail = tokensApi.detokenize(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, emptyList(),
                        emptyList(), ImmutableList.of(token), emptyList(), NO_OPTIONS, NO_ADHOC_REASON, false)
                .get(0).getFields().get("email").toString();
        assert "john@somemail.com".equals(returnedEmail);
    }

    private static void queryAllObjectsWithPageSize(ObjectsApi objectsApi) throws ApiException {

        ObjectFieldsPage objectIdsPage =
                objectsApi.listObjects(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, NO_ADHOC_REASON,
                        false, 1, "", "", emptyList(), ImmutableSet.of(UNSAFE_OPTION), null);

        assert objectIdsPage.getResults().size() == 1;
        Map<String, Object> searchResult = objectIdsPage.getResults().get(0);

        print("object retrieved by search: ", searchResult.toString());
        assert "john@somemail.com".equals(searchResult.get("email"));
        assert "123-12-1234".equals(searchResult.get("ssn"));
        assert "+1121212123".equals(searchResult.get("phone_number"));
        assert "12345".equals(searchResult.get("zip_code_us"));
    }

    private static void queryPropertiesOfObjectsById(ObjectsApi objectsApi, UUID id) throws ApiException {

        ObjectFieldsPage objectIdsPage = objectsApi.listObjects(COLLECTION_NAME, APP_FUNCTIONALITY_REASON,
                NO_ADHOC_REASON, false, null, "", "", ImmutableList.of(id),
                NO_OPTIONS, ImmutableList.of("ssn"));

        assert objectIdsPage.getResults().size() == 1;
        Map<String, Object> searchResult = objectIdsPage.getResults().get(0);
        assert searchResult.size() == 1;

        print("ssn property retrieved by id: ", searchResult.toString());
        assert "123-12-1234".equals(searchResult.get("ssn"));
    }

    private static void getTransformedPropertiesOfObjects(ObjectsApi objectsApi, UUID id) throws ApiException {

        ObjectFieldsPage objectIdsPage = objectsApi.listObjects(COLLECTION_NAME, APP_FUNCTIONALITY_REASON,
                NO_ADHOC_REASON, false, null, "", "", ImmutableList.of(id),
                NO_OPTIONS, ImmutableList.of("ssn.mask", "email.mask", "phone_number.mask"));

        assert objectIdsPage.getResults().size() == 1;
        Map<String, Object> searchResult = objectIdsPage.getResults().get(0);

        print("transformed propertied retrieved: ", searchResult.toString());
        assert "j***@somemail.com".equals(searchResult.get("email.mask"));
        assert "***-**-1234".equals(searchResult.get("ssn.mask"));
        assert "*******2123".equals(searchResult.get("phone_number.mask"));
    }

    private static void deleteToken(TokensApi tokensApi, String token) throws ApiException {

        tokensApi.deleteTokens(COLLECTION_NAME, APP_FUNCTIONALITY_REASON, emptyList(), emptyList(),
                ImmutableList.of(token), null, NO_OPTIONS, NO_ADHOC_REASON, false);
    }

    private static void deleteObject(ObjectsApi objectsApi, UUID id) throws ApiException {

        objectsApi.deleteObjectById(COLLECTION_NAME, id, APP_FUNCTIONALITY_REASON,
                NO_OPTIONS, NO_ADHOC_REASON, false);
    }

    private static ApiClient getApiClient() {

        // Create configuration, bearer auth and client API
        ApiClient pvaultClient = Configuration.getDefaultApiClient();
        pvaultClient.setBasePath("http://localhost:" + PVAULT_ADDRESS);
        pvaultClient.setBearerToken("pvaultauth");
        pvaultClient.addDefaultHeader("Content-Type", "application/json");
        return pvaultClient;
    }

    private static Property buildProperty(
            String name, String dataTypeName, String description,
            boolean isUnique, boolean isNullable, boolean isEncrypted, boolean isIndex) {

        Property property = new Property();
        property.setName(name);
        property.setDataTypeName(dataTypeName);
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
        } catch (ApiException apiException) {
            if (apiException.getMessage().contains("java.net.ConnectException")) {
                print(apiException.getMessage() + "\n\nIs the Vault running?\n");
            } else {
                throw new RuntimeException(apiException);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
