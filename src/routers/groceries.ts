import express from "express";
import { checkCreds, credsFailed } from "../lib/authUtils";
import { getApiDB } from "../lib/Database";
import { unknownError } from "../lib/util";

const app = express();

app.get("/lists", (req, res) =>
  checkCreds(req)
    .then(() => {
      getApiDB().then(db => {
        db.collection("groceries")
          .distinct("list", {})
          .then(list => {
            res.status(200).json(list);
          })
          .catch(err => unknownError(err, res));
      });
    })
    .catch(() => credsFailed(res))
);

export default app;
