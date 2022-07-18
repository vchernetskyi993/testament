import { ethers } from "hardhat";
import { expect, use } from "chai";
import chaiAsPromised from "chai-as-promised";
import { Testament } from "../typechain-types";
import { loadFixture } from "@nomicfoundation/hardhat-network-helpers";

use(chaiAsPromised);

type EmptyState = {
  contract: Testament;
  issuer: string;
  inheritors: [string, string, string];
  notifiers: [string];
};

describe("Testament", () => {
  async function freshStateFixture(): Promise<EmptyState> {
    const [_owner, issuer, i0, i1, i2, n0] = await ethers.getSigners();

    const factory = await ethers.getContractFactory("Testament");
    const contract = await factory.deploy();

    return {
      contract: await contract.connect(issuer),
      issuer: await issuer.getAddress(),
      inheritors: [
        await i0.getAddress(),
        await i1.getAddress(),
        await i2.getAddress(),
      ],
      notifiers: [await n0.getAddress()],
    };
  }

  describe("Issue Testament", () => {
    it("Should issue testament", async () => {
      // given
      const { contract, issuer, inheritors, notifiers } = await loadFixture(
        freshStateFixture
      );
      const input = {
        inheritors,
        shares: [4000, 3000, 3000],
        notifiers,
      };

      // when
      await contract.issueTestament(input);

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
    });

    it("Should check inheritors non empty", async () => {
      // given
      const {
        contract,
        issuer: _issuer,
        inheritors: _inheritors,
        notifiers,
      } = await loadFixture(freshStateFixture);
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
      const {
        contract,
        issuer: _issuer,
        inheritors,
        notifiers,
      } = await loadFixture(freshStateFixture);
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
      const {
        contract,
        issuer: _issuer,
        inheritors,
        notifiers,
      } = await loadFixture(freshStateFixture);
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
      const {
        contract,
        issuer: _issuer,
        inheritors,
      } = await loadFixture(freshStateFixture);
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
      const {
        contract,
        issuer: _issuer,
        inheritors,
        notifiers,
      } = await loadFixture(freshStateFixture);
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
    it("Should transfer FTs", () => {});

    it("Should batch transfer FTs", () => {});

    it("Should not transfer testament", () => {});

    it("Should not batch transfer testaments", () => {});
  });
});
