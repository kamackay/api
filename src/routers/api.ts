import express from "express";
import authRouter from "./auth";
import { checkCreds, credsFailed } from "../lib/authUtils";

const app = express();

app.all("/", (req, res) =>
  checkCreds(req, res)
    .then(() => {
      res.send("hi");
    })
    .catch(() => credsFailed(res))
);

app.use("/auth", authRouter);

export default app;
