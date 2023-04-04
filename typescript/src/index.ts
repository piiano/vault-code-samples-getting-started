import {
  Collection,
  CollectionsClient,
  Object,
  ObjectsClient,
  QueryToken,
  TokenizeRequest,
  TokensClient,
  VaultClient,
  VaultClientOptions,
} from "@piiano/vault-client";

// Type definitions for Customer object
interface Customer extends Object {
  ssn: string;
  email: string;
  phone_number?: string;
  zip_code_us?: string;
}

interface CustomerResult extends Partial<Customer> {
  id: string;
}

const reason = "AppFunctionality";

export default async function vaultGettingStarted(options?: VaultClientOptions) {

  console.log("\n\n== Steps 1 + 2: Connect to Piiano vault and check status ==\n\n");
  const vaultClient = setupVaultClient(options);

  await checkVault(vaultClient);

  console.log("\n\n== Step 3: Create a collection ==\n\n");
  const collectionName = 'customers';
  const collection = await createCollection(vaultClient.collections, {
    name: collectionName,
    type: 'PERSONS',
    properties: [
      { name: "ssn", data_type_name: "SSN", is_nullable: true, description: "Social Security Number" },
      { name: "email", data_type_name: "EMAIL" },
      { name: "phone_number", data_type_name: "PHONE_NUMBER", is_nullable: true },
      { name: "zip_code_us", data_type_name: "ZIP_CODE_US", is_nullable: true },
    ],
  });

  console.log("\n\n== Step 4: Add data ==\n\n");
  const objects: Customer[] = [
    {
      ssn: "123-12-1234",
      email: "john@somemail.com",
      phone_number: "+1-121212123",
      zip_code_us: "12345",
    },
    {
      ssn: "123-12-1235",
      email: "mary@somemail.com",
      phone_number: "+1-121212123",
      zip_code_us: "12345",
    },
    {
      ssn: "123-12-1236",
      email: "eric@somemail.com",
      phone_number: "+1-121212123",
      zip_code_us: "12345",
    },
  ];

  const customers = await addCustomersToCollection(vaultClient.objects, collectionName, objects);

  console.log("\n\n== Step 5: Tokenize data ==\n\n");
  const tokenRequest: TokenizeRequest = {
    type: 'pointer',
    props: ["email"],
    object: {
      id: customers[0].id!,
    },
  };

  const searchTokenRequest: QueryToken = {
    object_ids: [customers[0].id!],
  };

  const tokenId = await tokenizeObject(
    vaultClient.tokens,
    collectionName,
    customers[0] as CustomerResult,
    tokenRequest,
    searchTokenRequest
  );

  console.log("\n\n== Step 6: Query your data ==\n\n");
  await queryObjects(vaultClient.objects, collection, customers, customers[0], "ssn");

  console.log("\n\n== Step 7: Delete data ==\n\n");
  await deleteObject(
    vaultClient.tokens,
    vaultClient.objects,
    collectionName, tokenId, customers[0].id!, searchTokenRequest);

  console.log("\n\nDone!\n");
}

function setupVaultClient(options?: VaultClientOptions): VaultClient {
  console.log("Setting up API client connection...");

  // Set up API client
  return new VaultClient(options)
}

async function checkVault(vaultClient: VaultClient) {
  console.log("Preparing vault...");

  // Check vault health
  console.log("\tChecking health of vault...");
  const health = await vaultClient.system.controlHealth();
  if (health.status !== "pass") throw new Error("Health check failed.");

  // Fetch all collections
  console.log("\tChecking vault is empty...");
  const collections = await vaultClient.collections.listCollections({});
  if (collections.length > 0) throw new Error('Vault is not empty. Please run this script on a clean instance.');
}

async function createCollection(collectionClient: CollectionsClient, schema: Collection): Promise<Collection> {
  console.log(`Adding collection ${schema.name}...`);
  // Create collection
  const collection = await collectionClient.addCollection({
    requestBody: schema
  });

  console.log(
    `Collection details:
\ttype: ${collection.type}
\tname: ${collection.name}
\tcreation time: ${collection.creation_time}`
  );

  return collection;
}

async function addCustomersToCollection(objectsClient: ObjectsClient, collection: string, objects: Customer[]) {
  console.log(`Adding objects to collection ${collection}...`);
  // Add objects to collection
  const resultObjects = await objectsClient.addObjects({
    collection, reason, requestBody: objects,
  });

  if (!resultObjects.ok) throw new Error("Failed to add objects to the collection.");

  console.log(
    `Added objects with the following ID's to collection ${collection}:${
      resultObjects.results.map((item) => `\n\t${item.id}`)}`
  );
  console.log(`Searching for object by email (john@somemail.com) in collection ${collection}...`);

  // Search for objects in collection by email
  const searchResult = await objectsClient.searchObjects({
    collection, reason,
    requestBody: {
      match: {email: "john@somemail.com"},
    },
    props: ["id", "email"]
  });

  if (searchResult.results.length === 0)
    throw new Error("Failed to search objects in the collection.");

  console.log(`Found the following objects in collection ${collection}:${searchResult.results.map(
      item => `\n\t${JSON.stringify(item)}`)}`
  );

  return resultObjects.results as CustomerResult[];
}

async function tokenizeObject(
  tokensClient: TokensClient,
  collection: string,
  object: CustomerResult,
  tokenRequest: TokenizeRequest,
  searchTokenRequest: QueryToken
) {
  console.log(`Tokenizing object with ID ${object.id} in collection ${collection}...`);
  // Tokenize object
  const tokens = await tokensClient.tokenize({
    collection, reason,
    requestBody: [tokenRequest]
  });

  if (!tokens[0]) throw new Error("Failed to tokenize object.");
  console.log(
    `Tokenized object with ID ${object.id} in collection ${collection} with token ID ${tokens[0].token_id}.`
  );

  // Search for token
  const searchTokens = await tokensClient.searchTokens({
    collection, reason,
    requestBody: searchTokenRequest
  });

  if (!searchTokens[0])
    throw new Error(`Failed to find a token for the object with ID ${object.id}.`);
  console.log(
    `Found token with ID ${searchTokens[0].token_id} in collection ${collection}.`
  );

  return tokens[0].token_id;
}

async function queryObjects(
  objectsClient: ObjectsClient,
  collection: Collection,
  objects: CustomerResult[],
  object: CustomerResult,
  prop: string
) {

  console.log(`Querying paginated objects in collection ${collection.name}...`);
  // Query all objects with pagination enabled
  const allObjects = await objectsClient.listObjects({
    collection: collection.name,
    reason,
    options: ["unsafe"]
  });

  if (allObjects.results.length === 0)
    throw new Error("Failed to query objects in the collection.");
  console.log(
    `Found the following objects in collection ${collection.name}:${allObjects.results.map(
      item => `\n\t${JSON.stringify(item)}`
    )}`
  );
  console.log(`Pagination:\n\t${JSON.stringify(allObjects.paging)}`);

  console.log(`\nQuerying for a specific object ssn by ID in collection ${collection.name}...`);

  // Query only one object with a specific prop
  const objectWithProp = await objectsClient.listObjects({
    collection: collection.name,
    reason,
    ids: [object.id!],
    props: [prop]
  });

  if (objectWithProp.results.length === 0)
    throw new Error(`Failed to query object-${object.id} in the collection with ${prop} prop.`);
  console.log(
    `Found the following objects in collection ${collection.name}:${objectWithProp.results.map(
      item => `\n\t${JSON.stringify(item)}`
    )}`
  );

  console.log(
    `\nQuerying all the details for a specific object by ID in collection ${collection.name}...`
  );

  // Query only one object with all the props
  const objectsWithAllProps = await objectsClient.listObjects({
    collection: collection.name,
    reason,
    ids: [object.id!],
    options: ["unsafe"]
  });
  if (objectsWithAllProps.results.length === 0)
    throw new Error(`Failed to query object-${object.id} in the collection.`);

  console.log(
    `Found the following objects in collection ${collection.name}:${
      objectsWithAllProps.results.map(item => `\n\t${JSON.stringify(item)}`)}`
  );

  // Query only one object with all the props masked
  const objectsWithMasks = await objectsClient.listObjects({
    collection: collection.name,
    reason,
    ids: [object.id!],
    props: collection.properties
      .filter(item => item.name !== "zip_code_us")
      .map(item => `${item.name}.mask`)
  });
  if (objectsWithMasks.results.length === 0)
    throw new Error(`Failed to query object-${object.id} in the collection.`);

  console.log(
    `Found the following objects in collection ${collection.name}:${objectsWithMasks.results.map(
      item => `\n\t${JSON.stringify(item)}`
    )}`
  );
}

async function deleteObject(
  tokensClient: TokensClient,
  objectsClient: ObjectsClient,
  collection: string,
  tokenId: string,
  objectId: string,
  searchTokenRequest: QueryToken
) {
  console.log(`Deleting token with ID ${tokenId} in collection ${collection}...`);

  // Delete token
  await tokensClient.deleteTokens({
    collection, reason,
    tokenIds: [tokenId],
  });
  const tokens = await tokensClient.searchTokens({
    collection, reason,
    requestBody: searchTokenRequest,
  });
  if (tokens.length !== 0)
    throw new Error(
      `Failed to delete token with ID ${tokenId} in collection ${collection}.`
    );

  console.log(`Deleting object with ID ${objectId} in collection ${collection}...`);

  // Delete object
  await objectsClient.deleteObjectById({
    collection,
    reason,
    id: objectId,
  });

  const error = await objectsClient.getObjectById({
    collection,
    reason,
    id: objectId,
    options: ["unsafe"]
  }).catch(e => e);

  if (error.status !== 404)
    throw new Error(`Failed to delete object with ID ${objectId} in collection ${collection}.`);
}
