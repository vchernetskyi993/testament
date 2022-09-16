import { Main } from "@daml.js/testament";
import { ContractId } from "@daml/types";
import { JSONRPCServer, SimpleJSONRPCMethod } from "json-rpc-2.0";
import { ledger as connectLedger } from "./ledger";
import config from "./config";

const {
  daml: {
    factoryId,
    parties: { bank, government },
  },
} = config;

type HolderId = {
  holder: string;
};

type AccountUpdate = {
  amount: string;
} & HolderId;

type HolderData = {
  possession: string;
  testament: {
    inheritors: { [inheritor: string]: number };
    announced: boolean;
    executed: boolean;
  };
} & HolderId;

function methods(): { [method: string]: SimpleJSONRPCMethod } {
  return {
    /**
     * Propose bank account creation.
     * Government party should approve created account before adding funds to it.
     * @param args HolderId
     * @returns contract id of account proposal
     */
    async createAccount(args): Promise<{ contractId: string }> {
      const { holder } = args as HolderId;
      const ledger = await connectLedger();
      const { contractId } = await ledger.create(Main.Account.CreateAccount, {
        holder,
        bank,
        government,
      });
      return { contractId };
    },

    /**
     * Store some amount of funds to holder account.
     * @param args Account
     */
    async storeFunds(args): Promise<void> {
      const { holder, amount } = args as AccountUpdate;
      const ledger = await connectLedger();
      await ledger.exerciseByKey(
        Main.Account.Account.AddFunds,
        { _1: bank, _2: holder },
        { amount }
      );
    },

    /**
     * Withdraw some amount from holder account.
     * @param args Account
     */
    async withdrawFunds(args): Promise<void> {
      const { holder, amount } = args as AccountUpdate;
      const ledger = await connectLedger();
      await ledger.exerciseByKey(
        Main.Account.Account.WithdrawFunds,
        { _1: bank, _2: holder },
        { amount }
      );
    },

    /**
     * Fetch account data.
     * @param args HolderId
     * @returns HolderData
     */
    async fetchAccount(args): Promise<HolderData> {
      const { holder } = args as HolderId;
      const ledger = await connectLedger();
      const { possession } = await ledger
        .fetchByKey(Main.Account.Account, { _1: bank, _2: holder })
        .then((event) => event?.payload!!);

      const { inheritors, announced, executed } = await ledger
        .fetchByKey(Main.Testament.Testament, { _1: government, _2: holder })
        .then((event) => event?.payload!!);

      return {
        holder,
        possession,
        testament: {
          inheritors: Object.fromEntries(
            inheritors.entriesArray().map(([i, s]) => [i, +s])
          ),
          announced,
          executed,
        },
      };
    },

    /**
     * Execute announced testament.
     * @param args HolderId
     */
    async executeTestament(args): Promise<void> {
      const { holder } = args as HolderId;
      const ledger = await connectLedger();
      await ledger.exercise(
        Main.Factory.TestamentFactory.ExecuteTestament,
        factoryId as ContractId<Main.Factory.TestamentFactory>,
        { issuer: holder }
      );
    },
  };
}

export function rpcApi(): JSONRPCServer {
  const server = new JSONRPCServer();

  Object.entries(methods()).forEach(([name, body]) =>
    server.addMethod(name, body)
  );

  return server;
}
