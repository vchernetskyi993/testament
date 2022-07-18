// SPDX-License-Identifier: MIT
pragma solidity ^0.8.9;

import "@openzeppelin/contracts/token/ERC1155/ERC1155.sol";

/**
    @title Testament contract
    @notice ERC-1155 implementation. Holds FTs (GOLD, SILVER, BRONZE) and NFTs (TESTAMENT).
        Anyone can create one (and only one!) testament. On death notice this contract 
        will split FTs according to the rules defined in owner's testament.
 */
contract Testament is ERC1155 {
  uint256 public constant GOLD = 0;
  uint256 public constant SILVER = 1;
  uint256 public constant BRONZE = 2;
  uint256 public constant TESTAMENT = 3;

  struct TestamentData {
    address[] inheritors;
    uint16[] shares;
    address[] notifiers;
  }

  mapping(address => TestamentData) private testaments;

  constructor() ERC1155("") {
    _mint(msg.sender, GOLD, 10**9, "");
    _mint(msg.sender, SILVER, 10**18, "");
    _mint(msg.sender, BRONZE, 10**27, "");
  }

  /**
    @notice Create new testament for the sender.
    @param testament testament view with: 
      1. inheritors - array of addresses that will inherit sender's FTs. 
      2. shares - array of integers that show how exactly to split posessions.
        Sum of all shares should be 10000. It means that only 2 decimals proximity is allowed. 
        For example, if you want to split FTs between 2 accounts by 60.45% and 29.55%, you should send [6045, 2955] shares.
      3. notifiers - array of addresses that will be able to send death notice (e.g. call `announceExecution`).
    @dev All arrays should not be empty.
      Inheritors should have the same length as shares.
      Shares should sum up to 10000.
      Testament should not already exist.
      Emits 'TestementIssued' event.
  */
  function issueTestament(TestamentData calldata testament) external {
    address issuer = msg.sender;
    testaments[issuer] = testament;
    _mint(issuer, TESTAMENT, 1, "");
  }

  function fetchTestament(address owner)
    external
    view
    returns (TestamentData memory)
  {
    return testaments[owner];
  }

  // TODO: disable testament transfers
}
