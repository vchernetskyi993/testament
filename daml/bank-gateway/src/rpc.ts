import { Main } from "@daml.js/testament";
import { JSONRPCServer, SimpleJSONRPCMethod } from "json-rpc-2.0";
import { ledger as connectLedger } from "./ledger";
import config from "./config";

const {
  daml: {
    parties: { bank, government },
  },
} = config;

function methods(): { [method: string]: SimpleJSONRPCMethod } {
  return {
    /**
     * Propose bank account creation.
     * Government party should approve created account before adding funds to it.
     * @param args { holder: string }
     */
    async createAccount(args): Promise<{ contractId: string }> {
      const { holder } = args as { holder: string };
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
     * @param args { holder: string; amount: string; }
     */
    async storeFunds(args): Promise<void> {
      const { holder, amount } = args as { holder: string; amount: string };
      const ledger = await connectLedger();
      await ledger.exerciseByKey(
        Main.Account.Account.AddFunds,
        { _1: bank, _2: holder },
        { amount }
      );
    },

    /**
     *
     * @param args
     */
    withdrawFunds(args): void {
      const { message } = args as { message: string };
      console.log(message);
    },

    /**
     *
     * @param args
     */
    fetchAccount(args): void {
      const { message } = args as { message: string };
      console.log(message);
    },

    /**
     *
     * @param args
     */
    executeTestament(args): void {
      const { message } = args as { message: string };
      console.log(message);
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
