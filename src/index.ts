import bodyParser from "body-parser";
import express from "express";
import morgan from "morgan";
import { logger } from "./lib/Logger";
import { args } from "./lib/util";
import api from "./routers/api";
const port = args.port || Number(process.env.PORT) || 9876;

const app = express();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(
  morgan("combined", {
    stream: {
      write: (message: string) => logger.log("http", message.trim())
    }
  })
);
app.use("/", api);

app.listen(port, () => logger.info(`listening on port ${port}`));
