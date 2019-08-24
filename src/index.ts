import express from "express";
import logger from 'winston';
import api from "./routers/ApiRouter";

const app = express();

app.use("api", api);

app.listen(9876, () => logger.info(`listening on port ${9876}`));