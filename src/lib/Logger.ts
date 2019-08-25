import { createLogger, transports } from "winston";
import winston = require("winston");

export const logger = createLogger({
  format: winston.format.combine(
    winston.format.simple(),
    winston.format.json(),
    winston.format.timestamp({ format: "YY-MM-DD HH:mm:ss:SSS" }),
    winston.format.printf(
      ({ level, message, timestamp }) => `${timestamp} [${level}] - ${message}`
    )
  ),
  transports: [
    new transports.File({
      level: "info",
      filename: `/logs/api.log`,
      handleExceptions: true,
      maxsize: 5242880, // 5MB
      maxFiles: 5
    }),
    new transports.Console({ level: "debug", handleExceptions: true })
  ],
  exitOnError: false
});
