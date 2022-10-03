import { Proxy } from "aws4-proxy";
import * as dotenv from "dotenv";
import { CredentialProviderChain } from "aws-sdk/global";

dotenv.config();

const {
  AWS_ETHEREUM_HTTP_ENDPOINT,
  PROXY_PORT,
  PROXY_HOST,
} = process.env;

async function main() {
  const credentials = await new CredentialProviderChain().resolvePromise();

  const proxy = new Proxy({
    service: "managedblockchain",
    region: "us-east-1",
    endpoint: AWS_ETHEREUM_HTTP_ENDPOINT!!,
    credentials,
  });
  proxy.on("error", (err) => {
    if (err.code === "EADDRINUSE") console.error(err.toString());
    else throw err;
  });
  proxy.listen(PROXY_PORT, PROXY_HOST, () => {
    console.log(`Listening on http://${PROXY_HOST}:${PROXY_PORT}/`);
  });
}

main();
