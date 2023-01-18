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
PVAULT_PORT = 8123
PVAULT_ADDRESS = f'http://localhost:{PVAULT_PORT}'


def check_db_is_clear(client):
    # Verify the DB is empty. For safety reasons, don't work on non empty DB
    collections_manager = collections_api.CollectionsApi(client)

    all_collections = list(collections_manager.list_collections())
    assert len(all_collections) == 0, \
        "Bailing out due to existence of collections from previous runs. Please clear or recreate the Vault from scratch."


def create_api_client():
    # Create configuration, bearer auth and client API
    config = configuration.Configuration(host=PVAULT_ADDRESS)
    config.api_key = {AUTHORIZATION_HEADER: PVAULT_AUTHENTICATION}
    config.api_key_prefix = {AUTHORIZATION_HEADER: "Bearer"}
    client = api_client.ApiClient(config, AUTHORIZATION_HEADER,
                                  config.get_api_key_with_prefix(AUTHORIZATION_HEADER))

    return client


def create_api_managers(client):
    print("Create managers - each one responsible for different actions in the api")

    collections_manager = collections_api.CollectionsApi(client)
    objects_manager = objects_api.ObjectsApi(client)
    tokens_manager = tokens_api.TokensApi(client)

    return collections_manager, objects_manager, tokens_manager


def add_customers_collection(collections_manager, props):
    # Note: Adding a collection with pvschema is not supported in the SDK
    #       Throughout this script we will use JSON exclusively

    customers_collection = models.Collection(
        name=COLLECTION_NAME,
        type="PERSONS",
        properties=props
    )
    print(f"Adding collection '{customers_collection.name}'\n")
    customers_collection = collections_manager.add_collection(customers_collection)

    # Check the collections has been added
    collection = collections_manager.get_collection(customers_collection.name)
    assert collection is not None
    print(f"Collection details: \n{collection}\n")

    return customers_collection


def add_objects_to_collection(objects_manager, customers_collection, customers):

    print(f"Adding customers 1, 2, 3: \n{customers}\nto collection '{customers_collection.name}'\n")

    customers_id = {objects_manager.add_object(collection=customers_collection.name, reason=APP_FUNCTIONALITY_REASON, object_fields=customer)["id"]: customer
                    for customer in customers}

    print(f"This is a key: value dictionary of customer_id: customer => \n\n{customers_id}\n")

    response = objects_manager.search_objects(
        collection=customers_collection.name,
        reason=APP_FUNCTIONALITY_REASON,
        query=models.Query(match=models.MatchMap(email="john@somemail.com")),
        props=["id"],
    )

    print(f"Search by email 'john@somemail.com' result: \n{response.results}")

    customer1_id_from_search = response.results[0]
    assert list(customers_id)[0] == customer1_id_from_search['id']

    return customers_id


def tokenize_customer(tokens_manager, customers_collection, customer, prop, token_request, search_token_request):
    token = tokens_manager.tokenize(customers_collection.name,
                                    APP_FUNCTIONALITY_REASON,
                                    [token_request])[0]
    print(f"Tokenize customer1 email result: \n{token}\n")

    token_ids = tokens_manager.search_tokens(customers_collection.name, APP_FUNCTIONALITY_REASON, search_token_request)

    assert token_ids[0].token_id == token.token_id, f"{token_ids[0].token_id = } != {token['token_id'] = }"

    detokenized = tokens_manager.detokenize(
        customers_collection.name,
        reason=APP_FUNCTIONALITY_REASON, token_ids=[token.token_id])

    print(f"Detokenize customer1 token result: \n{detokenized}")

    assert len(detokenized) == 1
    detokenized = detokenized[0]

    assert detokenized.token_id == token.token_id, f"{detokenized.token_id = } != {token.token_id = }"
    assert detokenized.fields[
               prop.name] == customer.email, f"{detokenized.fields[prop.name]} != {customer.email}"

    return token


def query_customers(objects_manager, customers_collection, customer1, customer1_id, prop):
    all_customers = objects_manager.list_objects(customers_collection.name, APP_FUNCTIONALITY_REASON, page_size=1,
                                                 options=['unsafe'])
    print(f"Paging result of listing customers collection with page size = 1: \n{all_customers.paging}\n")

    assert all_customers.paging.cursor
    assert all_customers.paging.size + all_customers.paging.remaining_count == 3
    assert len(all_customers.results) > 0
    customer = all_customers.results[0]
    orig_customer = customer1
    assert customer.email == orig_customer.email

    # Now getting only the SSN

    customer1_ssn_from_get = objects_manager.list_objects(
        customers_collection.name,
        APP_FUNCTIONALITY_REASON,
        props=[prop],
        ids=[customer1_id])
    assert len(customer1_ssn_from_get.results) > 0
    print(f"Get customer1 SSN only: \n{customer1_ssn_from_get.results[0]}\n")
    ssn_from_get = customer1_ssn_from_get.results[0]['ssn']

    assert ssn_from_get == customer1.ssn

    # Getting all the details of customer1

    customer1_from_get = objects_manager.list_objects(
        customers_collection.name,
        APP_FUNCTIONALITY_REASON,
        options=[UNSAFE_OPTION],
        ids=[customer1_id])

    assert len(customer1_from_get.results) > 0
    assert customer1_from_get.results[0].email == customer1.email
    print(f"Get all details of customer1: \n{customer1_from_get.results[0]}\n")

    # Getting Customer1's data with masks

    customer1_masked = objects_manager.list_objects(
        customers_collection.name,
        APP_FUNCTIONALITY_REASON,
        props=['ssn.mask', 'email.mask', 'phone_number.mask'],
        ids=[customer1_id])

    assert len(customer1_masked.results) > 0
    assert customer1_masked.results[0]['ssn.mask'] == '***-**-1234'
    print(f"Masked ssn values of customer1: \n{customer1_masked.results[0]}")


def delete_customers(tokens_manager, objects_manager, customers_collection, token_id, customer1_id, search_token_request):
    tokens_manager.delete_tokens(
        collection=customers_collection.name,
        reason=APP_FUNCTIONALITY_REASON,
        token_ids=[token_id.token_id])

    token_ids = tokens_manager.search_tokens(customers_collection.name, APP_FUNCTIONALITY_REASON, search_token_request)
    assert len(token_ids) == 0

    # Deleting the customer

    objects_manager.delete_object_by_id(
        customers_collection.name, id=customer1_id, reason=APP_FUNCTIONALITY_REASON)

    try:
        customer1_from_get = objects_manager.list_objects(
            customers_collection.name,
            APP_FUNCTIONALITY_REASON,
            options=[UNSAFE_OPTION],
            ids=[customer1_id])
    except openapi_client.exceptions.NotFoundException:
        pass
    else:
        raise Exception("Object still exists!")


def main():
    print('\n\n== Step 1: Create Api client ==\n\n')
    client = create_api_client()

    print('\n\n== Step 2: Create Api managers ==\n\n')
    collections_manager, objects_manager, tokens_manager = create_api_managers(client)

    try:
        check_db_is_clear(client)
    except Exception as e:
        print(e)
        # openapi client doesn't expose which exception was thrown so revert to this heuristic for clarity
        if "Connection refused" in str(e):
            print("Unable to connect to the Vault ({}). Is it up?".format(PVAULT_ADDRESS))
        return

    print('\n\n== Step 3: Create a collection ==\n\n')

    ssn_property = models.ModelProperty(name="ssn", pii_type_name="SSN", is_unique=True, description="Social security number")
    email_property = models.ModelProperty(name="email", pii_type_name="EMAIL")
    phone_number_property = models.ModelProperty(name="phone_number", pii_type_name="PHONE_NUMBER", is_nullable=True)
    zip_code_property = models.ModelProperty(name="zip_code_us", pii_type_name="ZIP_CODE_US", is_nullable=True)

    customers_collection = add_customers_collection(collections_manager, [ssn_property, email_property, phone_number_property, zip_code_property])

    print('\n\n== Step 4: Add data ==\n\n')

    customer1 = models.ObjectFields(ssn="123-12-1234", email="john@somemail.com", phone_number="+1-121212123", zip_code_us="12345")
    customer2 = models.ObjectFields(ssn="123-12-1235", email="mary@somemail.com", phone_number="+1-121212124", zip_code_us="12345")
    customer3 = models.ObjectFields(ssn="123-12-1236", email="eric@somemail.com", phone_number="+1-121212125", zip_code_us="12345")

    customers = add_objects_to_collection(objects_manager, customers_collection, [customer1, customer2, customer3])
    customer1_id = list(customers)[0]

    print('\n\n== Step 5: Tokenize data ==\n\n')

    token_request = models.TokenizeRequest(
        object=models.InputObject(id=customer1_id),
        props=[email_property.name],
        type=models.TokenType("pointer"))

    search_token_request = models.QueryToken(object_ids=[customer1_id])

    token_id = tokenize_customer(tokens_manager, customers_collection, customer1, email_property,
                                 token_request, search_token_request)

    print('\n\n== Step 6: Query your data ==\n\n')

    query_customers(objects_manager, customers_collection, customer1, customer1_id, ssn_property.name)

    print('\n\n== Step 7: Delete data ==\n\n')
    # Deleting the token
    delete_customers(tokens_manager, objects_manager, customers_collection, token_id, customer1_id, search_token_request)

    print('Done!\n')


if __name__ == '__main__':
    main()
