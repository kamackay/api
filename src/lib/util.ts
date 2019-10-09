import { Response } from "express";
import minimist from "minimist";
import { logger } from "../lib/Logger";

export const args = minimist(process.argv.slice(2));

export const unknownError = (err: any, res: Response) => {
  logger.error(err);
  res.status(500).send(`Error`);
};
