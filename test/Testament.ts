import { ethers } from "hardhat";
import { expect, use } from "chai";
import chaiAsPromised from "chai-as-promised";
import { Testament } from "../typechain-types";
import { loadFixture } from "@nomicfoundation/hardhat-network-helpers";
import { SignerWithAddress } from "@nomiclabs/hardhat-ethers/signers";
import { ContractReceipt, Event } from "ethers";

use(chaiAsPromised);

type State = {
  contract: Testament;
  deployer: string;
  issuer: string;
  issuerSigner: SignerWithAddress;
  inheritors: [string, string, string];
  notifiers: [string];
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
      notifiers: [await n0.getAddress()],
      deployer: await deployer.getAddress(),
    };
  }

  async function issuerCallerFixture(): Promise<State> {
    const state = await freshStateFixture();
    return {
      ...state,
      contract: await state.contract.connect(state.issuerSigner),
    };
  }

  async function issuedTestamentFixture(): Promise<State> {
    const state = await issuerCallerFixture();
    await state.contract.issueTestament({
      inheritors: state.inheritors,
      shares: [4000, 3000, 3000],
      notifiers: state.notifiers,
    });
    return state;
  }

  describe("Issue Testament", () => {
    it("Should issue testament", async () => {
      // given
      const { contract, issuer, inheritors, notifiers } = await loadFixture(
        issuerCallerFixture
      );
      const input = {
        inheritors,
        shares: [4000, 3000, 3000],
        notifiers,
      };

      // when
      const receipt = await contract
        .issueTestament(input)
        .then((t) => t.wait());

      // then
      const {
        inheritors: storedInheritors,
        shares: storedShares,
        notifiers: storedNotifiers,
      } = await contract.fetchTestament(issuer);
      expect(storedInheritors).to.be.deep.equal(input.inheritors);
      expect(storedShares).to.be.deep.equal(input.shares);
      expect(storedNotifiers).to.be.deep.equal(input.notifiers);

      const nftBalance = await contract.balanceOf(issuer, contract.TESTAMENT());
      expect(nftBalance.toNumber()).to.be.equal(1);

      expect(getEvent(receipt, "TestamentIssued")?.args?.issuer).to.be.equal(
        issuer
      );
    });

    it("Should check inheritors non empty", async () => {
      // given
      const { contract, notifiers } = await loadFixture(issuerCallerFixture);
      const input = {
        inheritors: [],
        shares: [4000, 3000, 3000],
        notifiers,
      };

      // when+then
      return expect(
        contract.issueTestament(input)
      ).to.be.eventually.rejectedWith(/empty/);
    });

    it("Inheritors and shares should have equal sizes", async () => {
      // given
      const { contract, inheritors, notifiers } = await loadFixture(
        issuerCallerFixture
      );
      const input = {
        inheritors,
        shares: [4000, 3000],
        notifiers,
      };

      // when+then
      return expect(
        contract.issueTestament(input)
      ).to.be.eventually.rejectedWith(/equal lengths/);
    });

    it("Should check shares total is 100%", async () => {
      // given
      const { contract, inheritors, notifiers } = await loadFixture(
        issuerCallerFixture
      );
      const input = {
        inheritors,
        shares: [4000, 3000, 2500],
        notifiers,
      };

      // when+then
      return expect(
        contract.issueTestament(input)
      ).to.be.eventually.rejectedWith(/10000/);
    });

    it("Should check notifiers not empty", async () => {
      // given
      const { contract, inheritors } = await loadFixture(issuerCallerFixture);
      const input = {
        inheritors,
        shares: [4000, 3000, 3000],
        notifiers: [],
      };

      // when+then
      return expect(
        contract.issueTestament(input)
      ).to.be.eventually.rejectedWith(/empty/);
    });

    it("Should check testament isn't already present", async () => {
      // given
      const { contract, inheritors, notifiers } = await loadFixture(
        issuerCallerFixture
      );
      const input = {
        inheritors,
        shares: [4000, 3000, 3000],
        notifiers,
      };

      // when
      await contract.issueTestament(input);

      // then
      return expect(
        contract.issueTestament(input)
      ).to.be.eventually.rejectedWith(/already issued/);
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

  function getEvent(receipt: ContractReceipt, type: string): Event | undefined {
    return receipt.events?.find((e) => e.event === type)!;
  }
});
