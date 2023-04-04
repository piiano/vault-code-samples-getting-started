import vaultGettingStarted from ".";
import {Vault} from "@piiano/testcontainers-vault";

describe('getting started', () => {
  const testVaultServer = new Vault({
    env: {
      // don't send logs and metrics to sentry and datadog during tests
      PVAULT_SENTRY_ENABLE: false,
      PVAULT_LOG_DATADOG_ENABLE: "none",
    }
  });

  let port: number;

  before(async function () {
    // increase the timeout for the before hook to allow pulling the vault image
    this.timeout(30000);
    this.slow(25000);
    // start the local test vault server on a random port
    port = await testVaultServer.start();
  });
  // stop the local test vault server after all tests
  after(testVaultServer.stop.bind(testVaultServer));

  it('should be able to run as an e2e test', async function () {
    await vaultGettingStarted({
      vaultURL: `http://localhost:${port}`,
      apiKey: "pvaultauth",
    });
  });
});
