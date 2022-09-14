import dotenv from "dotenv";
import { expressServer } from "./src/server";

dotenv.config();

const port = process.env.SERVER_PORT;
expressServer().listen(port, () => {
  console.log(`Server is listening on ${port}`);
});
