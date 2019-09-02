import crypto from "crypto";
import { Dictionary, Request, Response } from "express-serve-static-core";
import { getDB } from "./Database";

export const randomToken = () => crypto.randomBytes(64).toString("hex");

export const checkCreds = (
  req: Request<Dictionary<string>>,
  res: Response
): Promise<void> =>
  new Promise((resolve, reject) => {
    const db = getAuthDB();
    const { authorization } = req.headers;
    if (authorization) {
      db.allDocs()
        .then(docs => {
          if (docs.rows.map(val => val.id).indexOf(authorization) >= 0) {
            resolve();
          } else {
            reject();
          }
        })
        .catch(reject);
    } else {
      reject();
    }
  });

export const credsFailed = (res: Response) =>
  res.status(401).send("Bad Authentication");
