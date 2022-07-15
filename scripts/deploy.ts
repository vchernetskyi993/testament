import { ethers } from "hardhat";

async function main() {
  const factory = await ethers.getContractFactory("Testament");
  const contract = await factory.deploy();

  await contract.deployed();

  console.log("Contract deployed to:", contract.address);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
