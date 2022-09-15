import dotenv from "dotenv";

dotenv.config();

const appConfig = {
  port: process.env.SERVER_PORT ?? "4200",
  auth: {
    baseUrl: process.env.AUTH_SERVER_URL ?? required("AUTH_SERVER_URL"),
    username:
      process.env.AUTH_SERVER_USERNAME ?? required("AUTH_SERVER_USERNAME"),
    password:
      process.env.AUTH_SERVER_PASSWORD ?? required("AUTH_SERVER_PASSWORD"),
  },
  daml: {
    node: {
      url: process.env.DAML_JSON_API_URL ?? required("DAML_JSON_API_URL"),
    },
    parties: {
      bank: process.env.DAML_BANK_PARTY ?? required("DAML_BANK_PARTY"),
      government:
        process.env.DAML_GOVERNMENT_PARTY ?? required("DAML_GOVERNMENT_PARTY"),
    },
  },
};

function required(key: string | undefined): string {
  throw Error(`${key} is required`);
}

export default appConfig;
