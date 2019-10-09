import express, { Response } from "express";
import { sha512 } from "js-sha512";
import { getExpireTime, randomToken } from "../lib/authUtils";
import { getDB } from "../lib/Database";
import { logger } from "../lib/Logger";

const app = express();

const invalidCreds = (res: Response) =>
  res.status(401).send(`Invalid Credentials`);
const unknownError = (err: any, res: Response) => {
  logger.error(err);
  res.status(500).send(`Error`);
};

app.post("/login", (req, res) => {
  getDB()
    .then(db => db.db("api"))
    .then(db => {
      const { username, password } = req.body;
      const usersCollection = db.collection("users");
      const tokenCollection = db.collection("tokens");
      usersCollection
        .findOne({ username })
        .then(user => {
          if (!user) {
            invalidCreds(res);
            return;
          }
          const { password: actualPass } = user;
          if (actualPass === sha512(password)) {
            const now = new Date();
            // Successful Login
            tokenCollection.findOne({ username }).then(currentToken => {
              if (!currentToken || currentToken.timeout <= now.getTime()) {
                // User doesn't currently have a token, or it is expired
                logger.info(`Generating new token for '${username}'`);
                const tokenData = {
                  username,
                  token: randomToken(),
                  timeout: getExpireTime(now),
                  timeLoggedIn: now.getTime(),
                  timeLoggedInReadable: now.toISOString()
                };
                tokenCollection.updateOne(
                  { username },
                  { $set: tokenData },
                  { upsert: true },
                  err => {
                    if (err) unknownError(err, res);
                    else res.status(200).json(tokenData);
                  }
                );
              } else {
                // User has a token
                logger.info(`Sending '${username}' an unexpired token`);
                res.status(200).json({ ...currentToken, _id: undefined });
              }
            });
          } else {
            invalidCreds(res);
          }
        })
        .catch(err => invalidCreds(res));
    })
    .catch(err => res.status(500).json(err));
});

export default app;
