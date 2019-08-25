import express from "express";
import { randomToken } from "../lib/authUtils";

const app = express();

app.use("/", (req, res) => {
  res.json({ newToken: randomToken() });
});

export default app;
