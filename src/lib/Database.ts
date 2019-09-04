import mongodb from "mongodb";
import data from "../../creds.json";

export const getDB = (): Promise<mongodb.MongoClient> =>
  new Promise<mongodb.MongoClient>((resolve, reject) => {
    const uri = `mongodb+srv://admin:${data.password}@apicluster-tsly9.mongodb.net/test?retryWrites=true&w=majority`;
    const client = new mongodb.MongoClient(uri, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    client.connect((err, c) => {
      if (err) {
        reject(err);
      } else {
        resolve(c);
      }
    });
  });
