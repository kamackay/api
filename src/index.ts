import { logger } from "./lib/Logger";
import api from "./routers/ApiRouter";
import morgan from "morgan";
import express from "express";

const app = express();
app.use(morgan("combined"));

app.use("/api", api);

app.all("/", (req, res) => res.json({name: "keith"}))

app.listen(9876, () => logger.info(`listening on port ${9876}`));
