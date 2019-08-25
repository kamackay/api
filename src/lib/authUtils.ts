import crypto from "crypto";
import { Dictionary, Request, Response } from "express-serve-static-core";

export const randomToken = () => crypto.randomBytes(64).toString("hex");

export const checkCreds = (
  req: Request<Dictionary<string>>,
  res: Response
): Promise<void> =>
  new Promise((resolve, reject) => {
    const { authorization } = req.headers;
    if (authorization) {
      resolve();
    } else {
      reject();
    }
  });

export const credsFailed = (res: Response) =>
  res.status(401).send("Bad Authentication");
