const [owner, user] = await ethers
  .getSigners()
  .then(([owner, user]) =>
    Promise.all([owner.getAddress(), user.getAddress()])
  );

const factory = await ethers.getContractFactory("Testament");
const testament = await factory.attach(contractAddress);

const GOLD = await testament.GOLD();
const SILVER = await testament.SILVER();
const BRONZE = await testament.BRONZE();
