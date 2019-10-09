import express from "express";
import { checkCreds, credsFailed } from "../lib/authUtils";
import authRouter from "./auth";
import filesRouter from "./files";

const app = express();

app.all("/", (req, res) =>
  checkCreds(req, res)
    .then(() => {
      res.send("hi");
    })
    .catch(() => credsFailed(res))
);

app.use("/auth", authRouter);
app.use("/files", filesRouter);

export default app;
