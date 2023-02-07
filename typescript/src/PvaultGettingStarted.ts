import {
  ApiError,
  Collection,
  CollectionsService,
  ObjectFields,
  ObjectsService,
  OpenAPI,
  QueryToken,
  SystemService,
  TokenizeRequest,
  TokensService,
  TokenType,
} from "../vault_typescript_sdk";

interface CustomersCollectionObject extends ObjectFields {
  ssn: string;
  email: string;
  phone_number?: string;
  zip_code_us?: string;
}

interface RemoteCustomersCollectionObject extends Partial<CustomersCollectionObject> {
  id?: string;
}

class PvaultGettingStarted {
  constructor(private host: string, private port: string, private apiToken: string) {}

  public async run() {
    console.log("\n\n== Steps 1 + 2: Connect to Piiano vault and check status ==\n\n");
    this.setupApiClient();

    await this.prepareVault();

    console.log("\n\n== Step 3: Create a collection ==\n\n");
    const customersCollectionSchema: Collection = {
      name: "customers",
      type: Collection.type.PERSONS,
      properties: [
        {
          name: "ssn",
          pii_type_name: "SSN",
          is_unique: true,
          description: "Social Security Number",
        },
        {
          name: "email",
          pii_type_name: "EMAIL",
        },
        {
          name: "phone_number",
          pii_type_name: "PHONE_NUMBER",
          is_nullable: true,
        },
        {
          name: "zip_code_us",
          pii_type_name: "ZIP_CODE_US",
          is_nullable: true,
        },
      ],
    };
    const customersCollection = await this.createCollection(customersCollectionSchema, "json");

    console.log("\n\n== Step 4: Add data ==\n\n");
    const objects: CustomersCollectionObject[] = [
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

    const customers = await this.addObjectsToCollection(customersCollection, objects);

    console.log("\n\n== Step 5: Tokenize data ==\n\n");
    const tokenRequest: TokenizeRequest = {
      type: TokenType.POINTER,
      props: ["email"],
      object: {
        id: customers[0].id,
      },
    };

    const searchTokenRequest: QueryToken = {
      object_ids: [customers[0].id!],
    };

    const tokenId = await this.tokenizeObject(
      customersCollection,
      customers[0],
      tokenRequest,
      searchTokenRequest
    );

    console.log("\n\n== Step 6: Query your data ==\n\n");
    await this.queryObjects(customersCollection, customers, customers[0], "ssn");

    console.log("\n\n== Step 7: Delete data ==\n\n");
    await this.deleteObject(customersCollection, tokenId, customers[0].id!, searchTokenRequest);

    console.log("\n\nDone!\n");
  }

  public setupApiClient() {
    console.log("Setting up API client connection...");

    // Set up API client
    OpenAPI.BASE = `${this.host}:${this.port}`;
    OpenAPI.TOKEN = this.apiToken;
  }

  public async prepareVault() {
    console.log("Preparing vault...");
    console.log("\tChecking vault is empty...");

    // Fetch all collections
    const collections = await CollectionsService.listCollections();
    if (collections.length > 0) throw new Error('Vault is not empty. Please run this script on a clean instance.');

    console.log("\tChecking health of vault...");

    // Check vault health
    const health = await SystemService.controlHealth();

    if (health.status !== "pass") throw new Error("Health check failed.");
  }

  public async createCollection(schema: Collection, format: "json" | "pvschema" = "json") {
    console.log(`Adding collection ${schema.name}...`);

    // Create collection
    const collection = await CollectionsService.addCollection(schema, format);

    // Fetch collection by name
    const newCollection = await CollectionsService.getCollection(collection.name);

    if (!newCollection) throw new Error("Collection not found.");
    console.log(
      `Collection details:\n\ttype: ${collection.type}\n\tname: ${collection.name}\n\tcreation time: ${collection.creation_time}`
    );

    return collection;
  }

  public async addObjectsToCollection(
    collection: Collection,
    objects: CustomersCollectionObject[]
  ) {
    const _ = undefined;
    console.log(`Adding objects to collection ${collection.name}...`);

    // Add objects to collection
    const resultObjects = await ObjectsService.addObjects(
      collection.name,
      "AppFunctionality",
      objects,
      "json"
    );

    if (!resultObjects.ok) throw new Error("Failed to add objects to the collection.");
    console.log(
      `Added objects with the following ID's to collection ${
        collection.name
      }:${resultObjects.results.map((item) => `\n\t${item.id}`)}`
    );

    console.log(
      `Searching for object by email (john@somemail.com) in collection ${collection.name}...`
    );

    // Search for objects in collection by email
    //
    // Here you need to specify the last parameter of the searchObjects function,
    // however, you might want to skip the ones in between, hence the usage of _ = undefined
    // It would be a good idea to wrap these generated API functions in your own functions
    // based on your desired usage
    const searchResult = await ObjectsService.searchObjects(
      collection.name,
      "AppFunctionality",
      { match: { email: "john@somemail.com" } },
      _,
      _,
      _,
      _,
      _,
      _,
      ["id", "email"]
    );

    if (searchResult.results.length === 0)
      throw new Error("Failed to search objects in the collection.");
    console.log(
      `Found the following objects in collection ${collection.name}:${searchResult.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );

    return resultObjects.results;
  }

  public async tokenizeObject(
    collection: Collection,
    object: RemoteCustomersCollectionObject,
    tokenRequest: TokenizeRequest,
    searchTokenRequest: QueryToken
  ) {
    console.log(`Tokenizing object with ID ${object.id} in collection ${collection.name}...`);

    // Tokenize object
    const tokens = await TokensService.tokenize(collection.name, "AppFunctionality", [
      tokenRequest,
    ]);

    if (!tokens[0]) throw new Error("Failed to tokenize object.");
    console.log(
      `Tokenized object with ID ${object.id} in collection ${collection.name} with token ID ${tokens[0].token_id}.`
    );

    // Search for token
    const searchTokens = await TokensService.searchTokens(
      collection.name,
      "AppFunctionality",
      searchTokenRequest
    );

    if (!searchTokens[0])
      throw new Error(`Failed to find a token for the object with ID ${object.id}.`);
    console.log(
      `Found token with ID ${searchTokens[0].token_id} in collection ${collection.name}.`
    );

    return tokens[0].token_id;
  }

  public async queryObjects(
    collection: Collection,
    objects: RemoteCustomersCollectionObject[],
    object: RemoteCustomersCollectionObject,
    prop: string
  ) {
    const _ = undefined;

    console.log(`Querying paginated objects in collection ${collection.name}...`);

    // Query all objects with pagination enabled
    const allObjects = await ObjectsService.listObjects(
      collection.name,
      "AppFunctionality",
      _,
      _,
      1,
      _,
      _,
      _,
      ["unsafe"]
    );

    if (allObjects.results.length === 0)
      throw new Error("Failed to query objects in the collection.");
    console.log(
      `Found the following objects in collection ${collection.name}:${allObjects.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );
    console.log(`Pagination:\n\t${JSON.stringify(allObjects.paging)}`);

    console.log(`\nQuerying for a specific object by ID in collection ${collection.name}...`);

    // Query only one object with a specific prop
    const objectWithProp = await ObjectsService.listObjects(
      collection.name,
      "AppFunctionality",
      _,
      _,
      _,
      _,
      _,
      [object.id!],
      _,
      [prop]
    );

    if (objectWithProp.results.length === 0)
      throw new Error(`Failed to query object-${object.id} in the collection with ${prop} prop.`);
    console.log(
      `Found the following objects in collection ${collection.name}:${objectWithProp.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );

    console.log(
      `\nQuerying all the details for a specific object by ID in collection ${collection.name}...`
    );

    // Query only one object with all the props
    const objectsWithAllProps = await ObjectsService.listObjects(
      collection.name,
      "AppFunctionality",
      _,
      _,
      _,
      _,
      _,
      [object.id!],
      _,
      collection.properties.map((item) => item.name)
    );
    if (objectsWithAllProps.results.length === 0)
      throw new Error(`Failed to query object-${object.id} in the collection.`);

    console.log(
      `Found the following objects in collection ${
        collection.name
      }:${objectsWithAllProps.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );

    // Query only one object with all the props masked
    const objectsWithMasks = await ObjectsService.listObjects(
      collection.name,
      "AppFunctionality",
      _,
      _,
      _,
      _,
      _,
      [object.id!],
      _,
      collection.properties
        .map((item) => `${item.name}.mask`)
        .filter((item) => item !== "zip_code_us.mask")
    );
    if (objectsWithMasks.results.length === 0)
      throw new Error(`Failed to query object-${object.id} in the collection.`);

    console.log(
      `Found the following objects in collection ${collection.name}:${objectsWithMasks.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );
  }

  public async deleteObject(
    collection: Collection,
    tokenId: string,
    objectId: string,
    searchTokenRequest: QueryToken
  ) {
    const _ = undefined;
    console.log(`Deleting token with ID ${tokenId} in collection ${collection.name}...`);

    // Delete token
    await TokensService.deleteTokens(collection.name, "AppFunctionality", _, _, [tokenId]);
    const tokens = await TokensService.searchTokens(
      collection.name,
      "AppFunctionality",
      searchTokenRequest
    );
    if (tokens.length !== 0)
      throw new Error(
        `Failed to delete token with ID ${tokenId} in collection ${collection.name}.`
      );

    console.log(`Deleting object with ID ${objectId} in collection ${collection.name}...`);

    // Delete object
    const deleteResult = await ObjectsService.deleteObjectById(
      collection.name,
      objectId,
      "AppFunctionality"
    );
    try {
      await ObjectsService.getObjectById(collection.name, objectId, "AppFunctionality", _, _, _, [
        "unsafe",
      ]);
    } catch (error: ApiError | any) {
      if (error.body.error_code !== "PV3005")
        throw new Error(
          `Failed to delete object with ID ${objectId} in collection ${collection.name}.`
        );
    }
  }
}

// Initialize the Pvault instance
const pvaultGettingStarted = new PvaultGettingStarted(
  process.env.PVAULT_HOST || "http://localhost",
  process.env.PVAULT_PORT || "8123",
  process.env.PVAULT_TOKEN || "pvaultauth"
);

// Run the demo
pvaultGettingStarted.run().catch((error) => {
  console.error(error);
});
