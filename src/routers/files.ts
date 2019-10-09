import axios from "axios";
import express from "express";

const app = express();

app.all("/rules.lsrules", (req, res) => {
  axios
    .get(
      `https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts;showintro=0`
    )
    .then(r => r.data)
    .then((data: string) => {
      const rules = data
        .split("\n")
        .map(s => s.trim())
        .filter(line => line.startsWith("127.0.0.1"))
        .map(line => line.split(/\s/))
        .map(s => s[1])
        .map(server => {
          return {
            action: "deny",
            creationDate: 1570476664.522696,
            modificationDate: 1570629033.3118901,
            owner: "any",
            process: "any",
            "remote-domains": server
          };
        });

      res.json({
        rules,
        name: "Keith's Rules",
        description: "List of Rules Generated by Keith MacKay's API"
      });
    })
    .catch(err => res.status(500).json(err));
});

export default app;
