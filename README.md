# Blockchain Testament

This is a POC contract. No production use intended.

## Table of Contents

- [Overview](#overview)
  - [Example use case](#example-use-case)
  - [Why the Blockchain](#why-the-blockchain)
- [Usage](#usage)
- [Development](#development)
  - [Testing](#testing)
- [Changelog](./CHANGELOG.md)

## Overview

ERC-1155 contract that stores imaginary FTs and adds an ability for users to issue their testaments.
Their possessions will be split per testament rules in case of confirmed death.

In general testament hold the following info:

- issuer
- inheritors and their shares
- trusted accounts (i.e. the ones trusted to announce death of the owner)

### Example use case:

![use-case](./assets/use-case.png)

[Excalidraw source](./assets/use-case.excalidraw)

### Why the Blockchain?

We could store our testaments in Postgres? It's common and cheap. Why bother?

Blockchain is distributed, thus trustless.
Once John stores his testament, he is sure that it will work exactly the way stated in the contract.

![blockchain-network](./assets/blockchain-network.png)

[Excalidraw source](./assets/blockchain-network.excalidraw)

## Usage

1. Install dependencies: `npm i`

2. Start local node: `npx hardhat node`

3. Deploy `Testament` contract `npx hardhat run scripts/deploy.ts --network localhost`

4. Start hardhat console `npx hardhat console --network localhost`

5. Variables set up:

```javascript
// contract address from step 3
const contractAddress = "0xe7f1725e7734ce288f8367e1bb143e90bb3f0512"
.load scripts/console-helper.js
```

6. Interact with contract:

```javascript
// transfer 1000 GOLD from contract owner to contract user
await testament.safeTransferFrom(owner, user, GOLD, 1000, []);
// check user's gold balance
await testament.balanceOf(user, GOLD);
```

## Development

The following helper tools are used. Please, run those before raising the PR.

```bash
# hint solidity files
npx solhint 'contracts/**/*.sol'
npx solhint 'contracts/**/*.sol' --fix
# hint ts files
npx eslint '**/*.{js,ts}'
npx eslint '**/*.{js,ts}' --fix
# run prettier
npx prettier '**/*.{json,sol,md,ts,js}' --check
npx prettier '**/*.{json,sol,md,ts,js}' --write
```

On each build contract API docs are generated.
View them in the browser of your choice: `firefox docgen/index.html`.
Comments follow [Natspec format](https://docs.soliditylang.org/en/latest/natspec-format.html).

### Testing

Using Hardhat:

```bash
# run full suite
npx hardhat test
# run specific test
npx hardhat test --grep "Should issue testament"
```

Using Mocha directly (works with extensions like [Mocha Test Explorer](https://marketplace.visualstudio.com/items?itemName=hbenl.vscode-mocha-test-adapter)):

1. Compile contracts manually: `npx hardhat compile`

2. Test:

```bash
# full suite
npx mocha
# specific test
npx mocha --grep "Should issue testament"
```

<!-- TODO: etherscan verification section -->

<!-- TODO: testnet deployment section -->

<!-- TODO: deployment & usage prices estimations section -->
