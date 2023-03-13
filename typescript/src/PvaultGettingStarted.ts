import {
  Collection,
  CollectionsApi,
  ModelObject,
  ObjectsApi,
  QueryToken,
  SystemApi,
  TokenizeRequest,
  TokensApi,
  TokenType,
  CollectionTypeEnum,
  Configuration,
} from "../vault_typescript_sdk";

interface CustomersCollectionObject extends ModelObject {
  ssn: string;
  email: string;
  phone_number?: string;
  zip_code_us?: string;
}

interface RemoteCustomersCollectionObject extends Partial<CustomersCollectionObject> {
  id?: string;
}

class PvaultGettingStarted {
  constructor(private host: string, private port: string, private apiToken: string, private conf: any) {}
  public async run() {
    console.log("\n\n== Steps 1 + 2: Connect to Piiano vault and check status ==\n\n");
    this.setupApiClient();

    await this.prepareVault();

    console.log("\n\n== Step 3: Create a collection ==\n\n");
    const customersCollectionSchema: Collection = {
      name: "customers",
      type: CollectionTypeEnum.Persons,
      properties: [
        {
          name: "ssn",
          data_type_name: "SSN",
          is_nullable: true,
          description: "Social Security Number",
        },
        {
          name: "email",
          data_type_name: "EMAIL",
        },
        {
          name: "phone_number",
          data_type_name: "PHONE_NUMBER",
          is_nullable: true,
        },
        {
          name: "zip_code_us",
          data_type_name: "ZIP_CODE_US",
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

    const customers = await this.addObjectsToCollection(customersCollection.data, objects);

    console.log("\n\n== Step 5: Tokenize data ==\n\n");
    const tokenRequest: TokenizeRequest = {
      type: TokenType.Pointer,
      props: ["email"],
      object: {
        id: customers[0].id,
      },
    };

    const searchTokenRequest: QueryToken = {
      object_ids: [customers[0].id!],
    };

    const tokenId = await this.tokenizeObject(
      customersCollection.data,
      customers[0],
      tokenRequest,
      searchTokenRequest
    );

    console.log("\n\n== Step 6: Query your data ==\n\n");
    await this.queryObjects(customersCollection.data, customers, customers[0], "ssn");

    console.log("\n\n== Step 7: Delete data ==\n\n");
    await this.deleteObject(customersCollection.data, tokenId, customers[0].id!, searchTokenRequest);

    console.log("\n\nDone!\n");
  }

  public setupApiClient() {
    console.log("Setting up API client connection...");

    // Set up API client
    this.conf = new Configuration({
      basePath: `${this.host}:${this.port}`,
      accessToken: this.apiToken
    })
  }

  public async prepareVault() {
    console.log("Preparing vault...");
    console.log("\tChecking vault is empty...");

    // Fetch all collections
    const collection_client = new CollectionsApi(this.conf)
    const system_client = new SystemApi(this.conf)

    const collections = await collection_client.listCollections();
    if (collections.data.length > 0) throw new Error('Vault is not empty. Please run this script on a clean instance.');

    console.log("\tChecking health of vault...");

    // Check vault health
    const health = await system_client.controlHealth();

    if (health.data.status !== "pass") throw new Error("Health check failed.");
  }

  public async createCollection(schema: Collection, format: "json" | "pvschema" = "json") {
    console.log(`Adding collection ${schema.name}...`);
    const collection_client = new CollectionsApi(this.conf)
    // Create collection
    const collection = await collection_client.addCollection(schema, format);

    // Fetch collection by name
    const newCollection = await collection_client.getCollection(collection.data.name);

    if (!newCollection) throw new Error("Collection not found.");
    console.log(
      `Collection details:\n\ttype: ${collection.data.type}\n\tname: ${collection.data.name}\n\tcreation time: ${collection.data.creation_time}`
    );

    return collection;
  }

  public async addObjectsToCollection(
    collection: Collection,
    objects: CustomersCollectionObject[]
  ) {
    const _ = undefined;
    console.log(`Adding objects to collection ${collection.name}...`);
    const object_client = new ObjectsApi(this.conf)
    // Add objects to collection
    const resultObjects = await object_client.addObjects(
          collection.name,
          "AppFunctionality",
          objects,
    );

    if (!resultObjects.data.ok) throw new Error("Failed to add objects to the collection.");
    console.log(
      `Added objects with the following ID's to collection ${
        collection.name
      }:${resultObjects.data.results.map((item) => `\n\t${item.id}`)}`
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
    const searchResult = await object_client.searchObjects(
      collection.name,
      "AppFunctionality",
      {
        match: { email: "john@somemail.com" },
      },
      _,
      _,
      _,
      _,
      _,
      _,
      ["id", "email"]
    );

    if (searchResult.data.results.length === 0)
      throw new Error("Failed to search objects in the collection.");
    console.log(
      `Found the following objects in collection ${collection.name}:${searchResult.data.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );

    return resultObjects.data.results;
  }

  public async tokenizeObject(
    collection: Collection,
    object: RemoteCustomersCollectionObject,
    tokenRequest: TokenizeRequest,
    searchTokenRequest: QueryToken
  ) {
    console.log(`Tokenizing object with ID ${object.id} in collection ${collection.name}...`);
    const token_client = new TokensApi(this.conf)
    // Tokenize object
    const tokens = await token_client.tokenize(
      collection.name,
      "AppFunctionality",
      [
        tokenRequest
      ]
    );

    if (!tokens.data[0]) throw new Error("Failed to tokenize object.");
    console.log(
      `Tokenized object with ID ${object.id} in collection ${collection.name} with token ID ${tokens.data[0].token_id}.`
    );

    // Search for token
    const searchTokens = await token_client.searchTokens(
      collection.name,
      "AppFunctionality",
      searchTokenRequest
    );

    if (!searchTokens.data[0])
      throw new Error(`Failed to find a token for the object with ID ${object.id}.`);
    console.log(
      `Found token with ID ${searchTokens.data[0].token_id} in collection ${collection.name}.`
    );

    return tokens.data[0].token_id;
  }

  public async queryObjects(
    collection: Collection,
    objects: RemoteCustomersCollectionObject[],
    object: RemoteCustomersCollectionObject,
    prop: string
  ) {
    const _ = undefined;

    console.log(`Querying paginated objects in collection ${collection.name}...`);
    const object_client = new ObjectsApi(this.conf)
    // Query all objects with pagination enabled
    const allObjects = await object_client.listObjects(
      collection.name,
      "AppFunctionality",
      _,
      _,
      _,
      _,
      _,
      _,
      ["unsafe"]
    );

    if (allObjects.data.results.length === 0)
      throw new Error("Failed to query objects in the collection.");
    console.log(
      `Found the following objects in collection ${collection.name}:${allObjects.data.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );
    console.log(`Pagination:\n\t${JSON.stringify(allObjects.data.paging)}`);

    console.log(`\nQuerying for a specific object by ID in collection ${collection.name}...`);

    // Query only one object with a specific prop
    const objectWithProp = await object_client.listObjects(
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

    if (objectWithProp.data.results.length === 0)
      throw new Error(`Failed to query object-${object.id} in the collection with ${prop} prop.`);
    console.log(
      `Found the following objects in collection ${collection.name}:${objectWithProp.data.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );

    console.log(
      `\nQuerying all the details for a specific object by ID in collection ${collection.name}...`
    );

    // Query only one object with all the props
    const objectsWithAllProps = await object_client.listObjects(
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
    if (objectsWithAllProps.data.results.length === 0)
      throw new Error(`Failed to query object-${object.id} in the collection.`);

    console.log(
      `Found the following objects in collection ${
        collection.name
      }:${objectsWithAllProps.data.results.map(
        (item: RemoteCustomersCollectionObject) => `\n\t${JSON.stringify(item)}`
      )}`
    );

    // Query only one object with all the props masked
    const objectsWithMasks = await object_client.listObjects(
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
    if (objectsWithMasks.data.results.length === 0)
      throw new Error(`Failed to query object-${object.id} in the collection.`);

    console.log(
      `Found the following objects in collection ${collection.name}:${objectsWithMasks.data.results.map(
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

    const token_client = new TokensApi(this.conf)
    const object_client = new ObjectsApi(this.conf)

    // Delete token
    await token_client.deleteTokens(
      collection.name,
      "AppFunctionality",
      _,
      _,
      [tokenId],
    );
    const tokens = await token_client.searchTokens(
          collection.name,
          "AppFunctionality",
          searchTokenRequest,
    );
    if (tokens.data.length !== 0)
      throw new Error(
        `Failed to delete token with ID ${tokenId} in collection ${collection.name}.`
      );

    console.log(`Deleting object with ID ${objectId} in collection ${collection.name}...`);

    // Delete object
    const deleteResult = await object_client.deleteObjectById(
      collection.name,
      objectId,
    "AppFunctionality",
    );
    try {
      await object_client.getObjectById(
        collection.name,
        objectId,
          "AppFunctionality",
        _,
        _,
        _,
        ["unsafe"]
      );
    } catch (error: any) {
      if (error.response.status !== 404)
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
  process.env.PVAULT_TOKEN || "pvaultauth",
    null
);

// Run the demo
pvaultGettingStarted.run().catch((error) => {
  console.error(error);
});
