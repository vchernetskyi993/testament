import Ledger from "@daml/ledger";
import { jwt } from "./auth";
import config from "./config";

const { daml: { node: { url } } } = config;

export async function ledger(): Promise<Ledger> {
  return new Ledger({ token: await jwt(), httpBaseUrl: url });
}
