import { ethers } from "hardhat";
import { expect, use } from "chai";
import chaiAsPromised from "chai-as-promised";
import { Testament } from "../typechain-types";
import { loadFixture, time } from "@nomicfoundation/hardhat-network-helpers";
import { SignerWithAddress } from "@nomiclabs/hardhat-ethers/signers";
import { constants, ContractReceipt, ContractTransaction, Event } from "ethers";
import dayjs from "dayjs";
import duration from "dayjs/plugin/duration";

use(chaiAsPromised);
dayjs.extend(duration);

type State = {
  contract: Testament;
  deployer: string;
  deployerSignature: SignerWithAddress;
  issuer: string;
  issuerSigner: SignerWithAddress;
  inheritors: [string, string, string];
  inheritorSigner: SignerWithAddress;
  shares: [4000, 3000, 3000];
  notifiers: [string];
  notifierSigner: SignerWithAddress;
};

describe("Testament", () => {
  async function freshStateFixture(): Promise<State> {
    const [deployer, issuer, i0, i1, i2, n0] = await ethers.getSigners();

    const factory = await ethers.getContractFactory("Testament");
    const contract = await factory.deploy();

    return {
      contract,
      issuer: await issuer.getAddress(),
      issuerSigner: issuer,
      inheritors: [
        await i0.getAddress(),
        await i1.getAddress(),
        await i2.getAddress(),
      ],
      inheritorSigner: i0,
      shares: [4000, 3000, 3000],
      notifiers: [await n0.getAddress()],
      deployer: await deployer.getAddress(),
      deployerSignature: deployer,
      notifierSigner: n0,
    };
  }

  async function issuerCallerFixture(): Promise<State> {
    const state = await freshStateFixture();
    return {
      ...state,
      contract: state.contract.connect(state.issuerSigner),
    };
  }

  async function issuedTestamentFixture(): Promise<State> {
    const state = await issuerCallerFixture();
    await issueTestament(state);
    return state;
  }

  describe("Issue Testament", () => {
    it("Should issue testament", async () => {
      // given
      const state = await loadFixture(issuerCallerFixture);
      const { contract, issuer, inheritors, shares, notifiers } = state;

      // when
      const receipt = await issueTestament(state).then((t) => t.wait());

      // then
      const {
        inheritors: storedInheritors,
        shares: storedShares,
        notifiers: storedNotifiers,
      } = await contract.fetchTestament(issuer);
      expect(storedInheritors).to.be.deep.equal(inheritors);
      expect(storedShares).to.be.deep.equal(shares);
      expect(storedNotifiers).to.be.deep.equal(notifiers);

      const nftBalance = await contract.balanceOf(issuer, contract.TESTAMENT());
      expect(nftBalance.toNumber()).to.be.equal(1);

      expect(getEvent(receipt, "TestamentIssued")?.args?.issuer).to.be.equal(
        issuer
      );
    });

    it("Should check inheritors non empty", async () => {
      // given
      const state = await loadFixture(issuerCallerFixture);

      // when+then
      return expect(
        issueTestament(state, { inheritors: [] })
      ).to.be.eventually.rejectedWith(/empty/);
    });

    it("Inheritors and shares should have equal sizes", async () => {
      // given
      const state = await loadFixture(issuerCallerFixture);

      // when+then
      return expect(
        issueTestament(state, { shares: [4000, 3000] })
      ).to.be.eventually.rejectedWith(/equal lengths/);
    });

    it("Should check shares total is 100%", async () => {
      // given
      const state = await loadFixture(issuerCallerFixture);

      // when+then
      return expect(
        issueTestament(state, { shares: [4000, 3000, 2500] })
      ).to.be.eventually.rejectedWith(/10000/);
    });

    it("Should check notifiers not empty", async () => {
      // given
      const state = await loadFixture(issuerCallerFixture);

      // when+then
      return expect(
        issueTestament(state, { notifiers: [] })
      ).to.be.eventually.rejectedWith(/empty/);
    });

    it("Should check testament isn't already present", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);

      // when+then
      return expect(issueTestament(state)).to.be.eventually.rejectedWith(
        /already issued/
      );
    });
  });

  describe("Transfer Testament", () => {
    it("Should transfer FTs", async () => {
      // given
      const { contract, deployer, issuer } = await loadFixture(
        freshStateFixture
      );
      const amount = 1000;

      // when
      await contract.safeTransferFrom(
        deployer,
        issuer,
        contract.GOLD(),
        amount,
        []
      );

      // then
      const possession = await contract.balanceOf(issuer, contract.GOLD());
      expect(possession.toNumber()).to.be.equal(amount);
    });

    it("Should batch transfer FTs", async () => {
      // given
      const { contract, deployer, issuer } = await loadFixture(
        freshStateFixture
      );
      const bronzeAmount = 9000;
      const goldAmount = 1000;

      // when
      await contract.safeBatchTransferFrom(
        deployer,
        issuer,
        [contract.BRONZE(), contract.GOLD()],
        [bronzeAmount, goldAmount],
        []
      );

      // then
      const goldPossession = await contract.balanceOf(issuer, contract.GOLD());
      expect(goldPossession.toNumber()).to.be.equal(goldAmount);
      const bronzePossession = await contract.balanceOf(
        issuer,
        contract.BRONZE()
      );
      expect(bronzePossession.toNumber()).to.be.equal(bronzeAmount);
    });

    it("Should not transfer testament", async () => {
      // given
      const { contract, deployer, issuer } = await loadFixture(
        issuedTestamentFixture
      );

      // when+then
      return expect(
        contract.safeTransferFrom(issuer, deployer, contract.TESTAMENT(), 1, [])
      ).to.be.rejectedWith(/non-transferable/);
    });

    it("Should not batch transfer testaments", async () => {
      // given
      const { contract, deployer, issuer } = await loadFixture(
        issuedTestamentFixture
      );

      // when+then
      return expect(
        contract.safeBatchTransferFrom(
          issuer,
          deployer,
          [contract.TESTAMENT()],
          [1],
          []
        )
      ).to.be.rejectedWith(/non-transferable/);
    });
  });

  describe("Announce Execution", () => {
    it("Should announce testament execution", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      const contract = state.contract.connect(state.notifierSigner);
      const timestamp = await time
        .latest()
        .then((t) => dayjs.unix(t).add(1, "minute").unix());
      await time.setNextBlockTimestamp(timestamp);

      // when
      const receipt = await contract
        .announceExecution(state.issuer)
        .then((t) => t.wait());

      // then
      const { executed, announcedAt, announcedBy } =
        await contract.fetchTestament(state.issuer);
      expect(executed).to.be.false;
      expect(announcedAt.toNumber()).to.be.equal(timestamp);
      expect(announcedBy).to.be.equal(state.notifiers[0]);

      const { issuer, announcer } = getEvent(receipt, "ExecutionAnnounced")
        ?.args as any as { issuer: string; announcer: string };
      expect(issuer).to.be.equal(state.issuer);
      expect(announcer).to.be.equal(state.notifiers[0]);
    });

    it("Should check if account is trusted to announce", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      const contract = state.contract.connect(state.inheritorSigner);

      // when+then
      return expect(
        contract.announceExecution(state.issuer)
      ).to.be.rejectedWith(/not trusted/);
    });

    it("Execution should not be already announced", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      const contract = state.contract.connect(state.notifierSigner);
      await contract.announceExecution(state.issuer);

      // when+then
      return expect(
        contract.announceExecution(state.issuer)
      ).to.be.rejectedWith(/announced/);
    });
  });

  describe("Cancel Execution", () => {
    it("Should decline announcement", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      await announceExecution(state);

      // when
      const receipt = await state.contract
        .declineExecution()
        .then((t) => t.wait());

      // then
      const { executed, announcedAt, announcedBy } =
        await state.contract.fetchTestament(state.issuer);
      expect(executed).to.be.false;
      expect(announcedAt.toNumber()).to.be.equal(0);
      expect(announcedBy).to.be.equal(constants.AddressZero);

      const { issuer, announcer } = getEvent(receipt, "ExecutionDeclined")
        ?.args as any as { issuer: string; announcer: string };
      expect(issuer).to.be.equal(state.issuer);
      expect(announcer).to.be.equal(state.notifiers[0]);
    });

    it("Testament execution should be announced to decline", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);

      // when+then
      return expect(state.contract.declineExecution()).to.be.rejectedWith(
        /not announced/
      );
    });

    it("Testament should not be already executed", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      await announceExecution(state);
      await time.increase(dayjs.duration({ days: 2 }).asSeconds());
      await state.contract.execute(state.issuer);

      // when+then
      return expect(state.contract.declineExecution()).to.be.rejectedWith(
        /executed/
      );
    });
  });

  describe("Execute", () => {
    it("Should execute testament", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      await announceExecution(state);
      const { contract } = state;
      await state.contract
        .connect(state.deployerSignature)
        .safeBatchTransferFrom(
          state.deployer,
          state.issuer,
          [contract.GOLD(), contract.SILVER()],
          [3000, 7000],
          []
        );
      await time.increase(dayjs.duration({ days: 2 }).asSeconds());

      // when
      const receipt = await contract
        .execute(state.issuer)
        .then((t) => t.wait());

      // then
      const { executed } = await contract.fetchTestament(state.issuer);
      expect(executed).to.be.true;

      const { issuer } = getEvent(receipt, "TestamentExecuted")
        ?.args as any as { issuer: string; announcer: string };
      expect(issuer).to.be.equal(state.issuer);

      return Promise.all(
        state.inheritors.map(async (inheritor, i) => {
          const share = state.shares[i];
          const goldBalance = await contract.balanceOf(
            inheritor,
            contract.GOLD()
          );
          expect(goldBalance.toNumber()).to.be.equal((3000 * share) / 10000);
          const silverBalance = await contract.balanceOf(
            inheritor,
            contract.SILVER()
          );
          expect(silverBalance.toNumber()).to.be.equal((7000 * share) / 10000);
        })
      );
    });

    it("Testament execution should be announced to execute", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      await time.increase(dayjs.duration({ days: 2 }).asSeconds());

      // when+then
      return expect(state.contract.execute(state.issuer)).to.be.rejectedWith(
        /not announced/
      );
    });

    it("Delay should pass for execution", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      await announceExecution(state);

      // when+then
      return expect(state.contract.execute(state.issuer)).to.be.rejectedWith(
        /not passed/
      );
    });

    it("Testament should not be already executed", async () => {
      // given
      const state = await loadFixture(issuedTestamentFixture);
      await announceExecution(state);
      await time.increase(dayjs.duration({ days: 2 }).asSeconds());
      await state.contract.execute(state.issuer);

      // when+then
      return expect(state.contract.execute(state.issuer)).to.be.rejectedWith(
        /executed/
      );
    });
  });

  function issueTestament(
    state: State,
    inputOptions?: {
      inheritors?: string[];
      shares?: number[];
      notifiers?: string[];
    }
  ): Promise<ContractTransaction> {
    return state.contract.issueTestament({
      inheritors: inputOptions?.inheritors || state.inheritors,
      shares: inputOptions?.shares || state.shares,
      notifiers: inputOptions?.notifiers || state.notifiers,
      executionDelay: dayjs.duration({ days: 1 }).asSeconds(),
    });
  }

  async function announceExecution(state: State): Promise<void> {
    await time.increase(dayjs.duration({ minutes: 1 }).asSeconds());
    await state.contract
      .connect(state.notifierSigner)
      .announceExecution(state.issuer);
  }

  function getEvent(receipt: ContractReceipt, type: string): Event | undefined {
    return receipt.events?.find((e) => e.event === type)!;
  }
});
