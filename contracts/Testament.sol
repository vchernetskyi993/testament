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

    constructor() ERC1155("") {
        _mint(msg.sender, GOLD, 10**9, "");
        _mint(msg.sender, SILVER, 10**18, "");
        _mint(msg.sender, BRONZE, 10**27, "");
    }
}
