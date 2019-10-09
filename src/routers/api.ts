import express from "express";
import { checkCreds, credsFailed } from "../lib/authUtils";
import authRouter from "./auth";
import filesRouter from "./files";
import groceryRouter from "./groceries";

const app = express();
type RouterMap = { [key: string]: express.Express };

app.all("/", (req, res) =>
  checkCreds(req)
    .then(() => {
      res.send("hi");
    })
    .catch(() => credsFailed(res))
);

const routers = {
  auth: authRouter,
  files: filesRouter,
  groceries: groceryRouter
} as RouterMap;

Object.keys(routers).forEach(name => {
  app.use(`/${name}`, routers[name]);
});

export default app;
