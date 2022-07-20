// SPDX-License-Identifier: MIT
pragma solidity ^0.8.9;

import "@openzeppelin/contracts/token/ERC1155/ERC1155.sol";

/**
 * @title Testament contract
 * @notice ERC-1155 implementation. Holds FTs (GOLD, SILVER, BRONZE) and NFTs (TESTAMENT).
 *     Anyone can create one (and only one!) testament. On death notice this contract
 *     will split FTs according to the rules defined in owner's testament.
 */
contract Testament is ERC1155 {
    uint256 public constant GOLD = 0;
    uint256 public constant SILVER = 1;
    uint256 public constant BRONZE = 2;
    uint256 public constant TESTAMENT = 3;

    uint16 public constant SHARES_TOTAL = 10000;

    struct TestamentInput {
        address[] inheritors;
        uint16[] shares;
        address[] notifiers;
        uint256 executionDelay;
    }

    struct TestamentData {
        address[] inheritors;
        uint16[] shares;
        address[] notifiers;
        bool executed;
        uint256 announcedAt;
        address announcedBy;
        uint256 executionDelay;
    }

    mapping(address => TestamentData) private testaments;

    event TestamentIssued(address issuer);
    event ExecutionAnnounced(address issuer, address announcer);
    event ExecutionDeclined(address issuer, address announcer);
    event TestamentExecuted(address issuer);

    constructor() ERC1155("") {
        _mint(msg.sender, GOLD, 10**9, "");
        _mint(msg.sender, SILVER, 10**18, "");
        _mint(msg.sender, BRONZE, 10**27, "");
    }

    /**
     * @notice Create new testament for the sender.
     * @param testament testament view with:
     *   1. inheritors - array of addresses that will inherit sender's FTs.
     *   2. shares - array of integers that show how exactly to split posessions.
     *     Sum of all shares should be 10000. It means that only 2 decimals proximity is allowed.
     *     For example, if you want to split FTs between 2 accounts by 60.45% and 29.55%, you should send [6045, 2955] shares.
     *   3. notifiers - array of addresses that will be able to send death notice (e.g. call `announceExecution`).
     * @dev All arrays should not be empty.
     *   Inheritors should have the same length as shares.
     *   Shares should sum up to 10000.
     *   Testament should not already exist.
     *   Emits 'TestementIssued' event.
     */
    function issueTestament(TestamentInput calldata testament) external {
        require(
            testament.inheritors.length > 0,
            "Inheritors should not be empty"
        );
        require(
            testament.inheritors.length == testament.shares.length,
            "Inheritors and shares should have equal lengths"
        );
        uint16 shareSum = 0;
        for (uint16 i = 0; i < testament.shares.length; i++) {
            shareSum += testament.shares[i];
        }
        require(shareSum == SHARES_TOTAL, "Shares should sum up to 10000");
        require(
            testament.notifiers.length > 0,
            "Notifiers should not be empty"
        );
        address issuer = msg.sender;
        require(
            testaments[issuer].inheritors.length == 0,
            "Testament is already issued"
        );

        testaments[issuer] = inputToData(testament);
        _mint(issuer, TESTAMENT, 1, "");
        emit TestamentIssued(issuer);
    }

    /**
     * @notice Fetch testament of the provided issuer.
     * @param issuer address
     */
    function fetchTestament(address issuer)
        external
        view
        returns (TestamentData memory)
    {
        return testaments[issuer];
    }

    /**
     * @dev See {IERC1155-safeTransferFrom}. Disabled testament transfers.
     */
    function safeTransferFrom(
        address from,
        address to,
        uint256 id,
        uint256 amount,
        bytes memory data
    ) public virtual override {
        require(id != TESTAMENT, "Testaments are non-transferable");
        require(!testaments[to].executed, "Target account should be alive");
        super.safeTransferFrom(from, to, id, amount, data);
    }

    /**
     * @dev See {IERC1155-safeBatchTransferFrom}. Disabled testament transfers.
     */
    function safeBatchTransferFrom(
        address from,
        address to,
        uint256[] memory ids,
        uint256[] memory amounts,
        bytes memory data
    ) public virtual override {
        for (uint256 i = 0; i < ids.length; i++) {
            require(ids[i] != TESTAMENT, "Testaments are non-transferable");
        }
        require(!testaments[to].executed, "Target account should be alive");
        super.safeBatchTransferFrom(from, to, ids, amounts, data);
    }

    /**
     * @notice Notify blockchain network about the death of the testament issuer.
     * @param issuer address
     * @dev Message sender should be one of the trusted accounts.
     *   Testament should not be already announced.
     *   Emits 'ExecutionAnnounced' event.
     */
    function announceExecution(address issuer) external {
        address announcer = msg.sender;
        TestamentData storage data = testaments[issuer];

        require(!isAnnounced(data), "Execution is already announced");
        require(
            contains(data.notifiers, announcer),
            "Announcer is not trusted"
        );

        data.announcedAt = block.timestamp;
        data.announcedBy = announcer;
        emit ExecutionAnnounced(issuer, announcer);
    }

    /**
     * @notice Decline announcement of your death.
     * @dev Message sender should be the issuer of announced testament.
     *   Testament should not be executed yet.
     *   Emits 'ExecutionCanceled' event.
     */
    function declineExecution() external {
        address issuer = msg.sender;
        TestamentData storage data = testaments[issuer];
        address announcer = data.announcedBy;

        require(isAnnounced(data), "Testament is not announced");
        require(!data.executed, "Testament is already executed");

        data.announcedAt = 0;
        data.announcedBy = address(0);
        emit ExecutionDeclined(issuer, announcer);
    }

    /**
     * @notice Execute testament rules (i.e. splits issuer's FTs).
     *   After 'executionDelay' time has passed anyone can trigger this function.
     * @dev Testament should be announced.
     *   Execution delay should pass.
     *   Testament should not be executed yet.
     *   Emits 'TestamentExecuted' event.
     */
    function execute(address issuer) external {
        TestamentData storage data = testaments[issuer];

        require(isAnnounced(data), "Testament is not announced");
        require(
            data.announcedAt + data.executionDelay < block.timestamp,
            "Delay before exectution has not passed"
        );
        require(!data.executed, "Testament is already executed");

        uint256[] memory tokens = fungibleTokens();
        for (uint256 i = 0; i < tokens.length; i++) {
            distributeFungibleToken(issuer, tokens[i], data);
        }
        data.executed = true;
        emit TestamentExecuted(issuer);
    }

    function distributeFungibleToken(
        address issuer,
        uint256 token,
        TestamentData memory data
    ) private {
        uint256 balance = balanceOf(issuer, token);
        for (uint256 i = 0; i < data.inheritors.length; i++) {
            address inheritor = data.inheritors[i];
            uint16 share = data.shares[i];
            uint256 transfer = (balance * share) / SHARES_TOTAL;
            _safeTransferFrom(issuer, inheritor, token, transfer, "");
        }
    }

    function fungibleTokens() private pure returns (uint256[] memory) {
        uint256[] memory result = new uint256[](3);
        result[0] = GOLD;
        result[1] = SILVER;
        result[2] = BRONZE;
        return result;
    }

    function isAnnounced(TestamentData memory data)
        private
        pure
        returns (bool)
    {
        return data.announcedAt != 0;
    }

    function inputToData(TestamentInput calldata input)
        private
        pure
        returns (TestamentData memory)
    {
        return
            TestamentData(
                input.inheritors,
                input.shares,
                input.notifiers,
                false,
                0,
                address(0),
                input.executionDelay
            );
    }

    function contains(address[] memory arr, address item)
        private
        pure
        returns (bool)
    {
        for (uint256 i = 0; i < arr.length; i++) {
            if (arr[i] == item) {
                return true;
            }
        }
        return false;
    }
}
