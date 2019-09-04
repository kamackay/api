import winston, { createLogger, transports } from "winston";

export const logger = createLogger({
  format: winston.format.combine(
    winston.format.timestamp({
      format: "YYYY-MM-DD HH:mm:ss:SSS"
    }),
    winston.format.json({ space: 2 }),
    winston.format.prettyPrint({ colorize: true, depth: 4 }),
    winston.format.colorize(),
    winston.format.printf(
      ({ level, message, timestamp }) =>
        `${timestamp} [${level}] - ${
          typeof message === "object"
            ? JSON.stringify(message, null, 2)
            : message
        }`
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
