import express from "express";

const app = express();

app.use("/login", (req, res) => {
  // const db = getAuthDB();
  // const token = randomToken();
  // db.put({ _id: token });
  // res.json({ newToken: token });
});

export default app;
