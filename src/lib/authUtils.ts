import crypto from "crypto";
import { Dictionary, Request, Response } from "express-serve-static-core";
import { getDB } from "./Database";
import { logger } from "./Logger";

export const randomToken = () => crypto.randomBytes(64).toString("hex");

export const checkCreds = (
  req: Request<Dictionary<string>>,
  res: Response
): Promise<void> =>
  new Promise((resolve, reject) => {
    getDB()
      .then(db => {
        const { authorization } = req.headers;
        const collection = db.db("api").collection("tokens");
        // Check given token
        collection
          .findOne({ token: authorization })
          .then(authToken => {
            logger.info({ authToken });
            if (authToken && authToken.token === authorization) {
              if (authToken.timeout > new Date().getTime()) {
                reject("Token has timed out");
              } else resolve();
            } else {
              reject("Invalid Token");
            }
          })
          .catch(reject);
      })
      .catch(reject);
  });

export const credsFailed = (res: Response) =>
  res.status(401).send("Bad Authentication");
