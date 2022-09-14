import { JSONRPCServer, SimpleJSONRPCMethod } from "json-rpc-2.0";

const methods: { [method: string]: SimpleJSONRPCMethod } = {

  /**
   * Propose bank account creation. Government party should approve new accounts.
   * @param args 
   */
  createAccount(args): Promise<void> {
    const { text } = args as { text: string };

    return Promise.resolve();
  },

  /**
   * 
   * @param args 
   */
  storeFunds(args): void {
    const { message } = args as { message: string };
    console.log(message);
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

export function rpcApi(): JSONRPCServer {
  const server = new JSONRPCServer();

  Object.entries(methods).forEach(([name, body]) =>
    server.addMethod(name, body)
  );

  return server;
}
