from openapi_client.api import collections_api
from openapi_client.api import objects_api
from openapi_client.api import tokens_api
from openapi_client import api_client
from openapi_client import configuration
from openapi_client import models
import openapi_client

COLLECTION_NAME = 'customers'
APP_FUNCTIONALITY_REASON = "AppFunctionality"
AUTHORIZATION_HEADER = "Authorization"
UNSAFE_OPTION = "unsafe"
PVAULT_AUTHENTICATION = "pvaultauth"

PVAULT_ADDRESS = 'http://localhost:8123'


# Verify the DB is empty. For safety reasons, don't work on non empty DB
def check_clear(client):
    collections_manager = collections_api.CollectionsApi(client)

    all_collections = list(collections_manager.list_collections())
    assert len(collections_manager.list_collections()) == 0, \
        "Bailing out due to existence of collections from previous runs. Please clear or recreate the Vault from scratch."


def main():
    # Create configuration, bearer auth and client API
    config = configuration.Configuration(host=PVAULT_ADDRESS)
    config.api_key = {AUTHORIZATION_HEADER: PVAULT_AUTHENTICATION}
    config.api_key_prefix = {AUTHORIZATION_HEADER: "Bearer"}
    client = api_client.ApiClient(config, AUTHORIZATION_HEADER,
                                  config.get_api_key_with_prefix(AUTHORIZATION_HEADER))

    # Create managers - each one responsible for different actions
    collections_manager = collections_api.CollectionsApi(client)
    objects_manager = objects_api.ObjectsApi(client)
    tokens_manager = tokens_api.TokensApi(client)

    try:
        check_clear(client)
    except Exception as e:
        print(e)
        # openapi client doesn't expose which exception was thrown so revert to this heuristic for clarity
        if "Connection refused" in str(e):
            print("Unable to connect to the Vault ({}). Is it up?".format(PVAULT_ADDRESS))
        return

    print('\n\n== Step 3: Create a collection ==\n\n')

    # Note: Adding a collection with pvschema is not supported in the SDK
    #       Throughout this script we will use JSON exclusively

    ssn_property = models.ModelProperty(name="ssn", pii_type_name="SSN", is_unique=True, description="Social security number")
    email_property = models.ModelProperty(name="email", pii_type_name="EMAIL")
    customers_collection = models.Collection(
        name=COLLECTION_NAME,
        type="PERSONS",
        properties=[
            ssn_property,
            email_property,
            models.ModelProperty(name="phone_number", pii_type_name="PHONE_NUMBER", is_nullable=True),
            models.ModelProperty(name="zip_code_us", pii_type_name="ZIP_CODE_US", is_nullable=True),
        ]
    )

    customers_collection = collections_manager.add_collection(customers_collection)

    # Check the collections has been added
    collection = collections_manager.get_collection(customers_collection.name)
    assert collection is not None
    print(collection)

    print('\n\n== Step 4: Add data ==\n\n')

    customer1 = models.ObjectFields(ssn="123-12-1234", email="john@somemail.com", phone_number="+1-121212123", zip_code_us="12345")
    customer2 = models.ObjectFields(ssn="123-12-1235", email="mary@somemail.com", phone_number="+1-121212124", zip_code_us="12345")
    customer3 = models.ObjectFields(ssn="123-12-1236", email="eric@somemail.com", phone_number="+1-121212125", zip_code_us="12345")

    customer1_id = objects_manager.add_object(collection=customers_collection.name, reason=APP_FUNCTIONALITY_REASON, object=customer1)
    print(customer1_id)
    customer2_id = objects_manager.add_object(collection=customers_collection.name, reason=APP_FUNCTIONALITY_REASON, object=customer2)
    print(customer2_id)
    customer3_id = objects_manager.add_object(collection=customers_collection.name, reason=APP_FUNCTIONALITY_REASON, object=customer3)
    print(customer3_id)

    id_to_customer = {customer1_id.id: customer1, customer2_id.id: customer2, customer3_id.id: customer3}

    response = objects_manager.search_objects(
        collection=customers_collection.name, 
        reason=APP_FUNCTIONALITY_REASON, 
        query=models.Query(match=models.QueryMap(email="john@somemail.com")),
        props=["id"],
    )

    print(response.results)

    customer1_id_from_search = response.results[0]
    assert customer1_id['id'] == customer1_id_from_search['id'], (customer1_id, customer1_id_from_search)

    print('\n\n== Step 5: Tokenize data ==\n\n')

    token_request = models.TokenizeRequest(
        object_ids=[customer1_id.id],
        props=[email_property.name], type="POINTER")
    token_id = tokens_manager.tokenize(customers_collection.name,
                                       APP_FUNCTIONALITY_REASON,
                                       token_request)[0]
    print("Token:", token_id)

    search_token_request = models.QueryToken(object_ids=[customer1_id.id])
    token_ids = tokens_manager.search_tokens(customers_collection.name, APP_FUNCTIONALITY_REASON, search_token_request)

    assert token_ids[0].token_id == token_id['token_id'], f"{token_ids[0].token_id = } != {token_id['token_id'] = }"

    detokenized = tokens_manager.detokenize(
        customers_collection.name,
        reason=APP_FUNCTIONALITY_REASON, token_ids=[token_id.token_id])
    
    assert len(detokenized) == 1
    detokenized = detokenized[0]

    assert detokenized.token_id == token_id.token_id, f"{detokenized.token_id = } != {token_id.token_id = }"
    assert detokenized.fields[email_property.name] == customer1.email, f"{detokenized.fields[email_property.name]} != {customer1.email}"

    print('\n\n== Step 6: Query your data ==\n\n')

    all_customers = objects_manager.list_objects(customers_collection.name, APP_FUNCTIONALITY_REASON, page_size=1, options=['unsafe'])
    print(all_customers)
    assert all_customers.paging.cursor
    assert all_customers.paging.size + all_customers.paging.remaining_count == 3
    assert len(all_customers.results) > 0
    customer = all_customers.results[0]
    orig_customer = id_to_customer[customer.id]
    assert customer.email == orig_customer.email

    # Now getting only the SSN
    
    customer1_ssn_from_get = objects_manager.list_objects(
        customers_collection.name, 
        APP_FUNCTIONALITY_REASON, 
        props=[ssn_property.name],
        ids=[customer1_id.id])
    assert len(customer1_ssn_from_get.results) > 0
    print(customer1_ssn_from_get.results[0])
    ssn_from_get = customer1_ssn_from_get.results[0]['ssn']
    
    assert ssn_from_get == customer1.ssn

    # Getting all the details of customer1

    customer1_from_get = objects_manager.list_objects(
        customers_collection.name, 
        APP_FUNCTIONALITY_REASON, 
        options=[UNSAFE_OPTION],
        ids=[customer1_id.id])
    
    assert len(customer1_from_get.results) > 0
    assert customer1_from_get.results[0].email == customer1.email
    print(customer1_from_get.results[0])

    # Getting Customer1's data with masks

    customer1_masked = objects_manager.list_objects(
        customers_collection.name, 
        APP_FUNCTIONALITY_REASON, 
        props=['ssn.mask', 'email.mask', 'phone_number.mask'],
        ids=[customer1_id.id])
    
    assert len(customer1_masked.results) > 0
    assert customer1_masked.results[0]['ssn.mask'] == '***-**-1234'
    print(customer1_masked.results[0])

    print('\n\n== Step 7: Delete data ==\n\n')
    # Deleting the token

    tokens_manager.delete_tokens(
        collection=customers_collection.name,
        reason=APP_FUNCTIONALITY_REASON,
        token_ids=[token_id.token_id])
    
    token_ids = tokens_manager.search_tokens(customers_collection.name, APP_FUNCTIONALITY_REASON, search_token_request)
    assert len(token_ids) == 0

    # Deleting the customer

    objects_manager.delete_object_by_id(
        customers_collection.name, id=customer1_id.id, reason=APP_FUNCTIONALITY_REASON)
    
    try:
        customer1_from_get = objects_manager.list_objects(
            customers_collection.name, 
            APP_FUNCTIONALITY_REASON, 
            options=[UNSAFE_OPTION],
            ids=[customer1_id.id])
    except openapi_client.exceptions.NotFoundException:
        pass
    else:
        raise Exception("Object still exists!")
    print('Done!\n')


if __name__ == '__main__':
    main()
