# Blockchain Testament

This is a POC contract. No production use intended.

## Overview

ERC-1155 contract that stores imaginary FTs and adds an ability for users to issue their testaments.
Their possessions will be split per testament rules in case of confirmed death.

In general testament hold the following info:
* issuer
* inheritors and their shares
* trusted accounts (i.e. the ones trusted to announce death of the owner)

## Example use case:

![use-case](./assets/use-case.png)

[Excalidraw source](./assets/use-case.excalidraw)

## Why the Blockchain?

We could store our testaments in Postgres? It's common and cheap. Why bother?

Blockchain is distributed, thus trustless. 
Once John stores his testament, he is sure that it will work exactly the way stated in the contract.

![blockchain-network](./assets/blockchain-network.png)

## Usage

<!-- TODO: add usage examples -->
