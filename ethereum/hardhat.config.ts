import { HardhatUserConfig } from "hardhat/config";
import "@nomicfoundation/hardhat-toolbox";
import "hardhat-docgen";
import * as dotenv from "dotenv";

dotenv.config();

const { GOERLI_ACCOUNT_PRIVATE_KEY, GOERLI_ETHERSCAN_API_KEY } = process.env;

const config: HardhatUserConfig = {
  solidity: "0.8.9",
  docgen: {
    clear: true,
    runOnCompile: true,
  },
  networks: {
    aws: {
      url: "http://127.0.0.1:3000/",
      accounts: [GOERLI_ACCOUNT_PRIVATE_KEY!!],
    },
  },
  etherscan: {
    apiKey: {
      goerli: GOERLI_ETHERSCAN_API_KEY!!,
    },
  },
};

export default config;
