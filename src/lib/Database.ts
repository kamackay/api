import PouchDB from "pouchdb";

export const getDB = (
  name: string,
  config?: Partial<PouchDB.Configuration.DatabaseConfiguration>
) => {
  PouchDB.plugin(require("pouchdb-adapter-cordova-sqlite"));
  PouchDB.plugin(require("pouchdb-adapter-memory"));

  return new PouchDB(name, { adapter: "sqlite", ...config });
};
