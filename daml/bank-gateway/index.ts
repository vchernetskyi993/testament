import config from "./src/config";
import { expressServer } from "./src/server";

expressServer().listen(config.port, () => {
  console.log(`Server is listening on ${config.port}`);
});
