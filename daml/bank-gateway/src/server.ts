import bodyParser from "body-parser";
import express, { Express } from "express";
import { rpcApi } from "./rpc";

export function expressServer(): Express {
  const app = express();
  app.use(bodyParser.json());

  const rpc = rpcApi();

  app.post("/json-rpc", (req, res) => {
    const jsonRPCRequest = req.body;

    rpc.receive(jsonRPCRequest).then((jsonRPCResponse) => {
      if (jsonRPCResponse) {
        res.json(jsonRPCResponse);
      } else {
        // If response is absent, it was a JSON-RPC notification method.
        // Respond with no content status (204).
        res.sendStatus(204);
      }
    });
  });

  return app;
}
