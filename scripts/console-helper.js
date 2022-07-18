const [_owner, userSigner] = await ethers.getSigners();

const [owner, user, wife, son, attorney] = await ethers
  .getSigners()
  .then((signers) => Promise.all(signers.map((signer) => signer.getAddress())));

const factory = await ethers.getContractFactory("Testament", userSigner);
const testament = await factory.attach(contractAddress);

const GOLD = await testament.GOLD();
const SILVER = await testament.SILVER();
const BRONZE = await testament.BRONZE();
