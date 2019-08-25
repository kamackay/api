import { logger } from "./lib/Logger";
import api from "./routers/api";
import morgan from "morgan";
import express from "express";
import { args } from "./lib/util";

const port = args.port || Number(process.env.PORT) || 9876;

const app = express();
app.use(
  morgan("combined", {
    stream: {
      write: (message: string) => logger.log("http", message.trim())
    }
  })
);
app.use("/api", api);

app.listen(port, () => logger.info(`listening on port ${port}`));
