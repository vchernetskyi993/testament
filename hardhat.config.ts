import { HardhatUserConfig } from "hardhat/config";
import "@nomicfoundation/hardhat-toolbox";
import "hardhat-docgen";

const config: HardhatUserConfig = {
  solidity: "0.8.9",
  docgen: {
    clear: true,
    runOnCompile: true,
  },
};

export default config;
