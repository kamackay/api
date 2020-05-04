package com.keithmackay.api.tasks;

import com.google.inject.Inject;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.model.Tuple;
import com.mongodb.client.MongoCollection;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import static com.keithmackay.api.utils.UtilsKt.*;

public class NewsPriorityTask extends Task {

  private final Logger log = getLogger(NewsPriorityTask.class);

  private final MongoCollection<Document> newsCollection;

  @Inject
  NewsPriorityTask(final Database db) {
    this.newsCollection = db.getCollection("news");
  }

  @Override
  public void run() {
    newsCollection
        // Find all documents that need to have priorities set
        .find(doc("priority", eq(-1)))
        .into(threadSafeList())
        .stream()
        .map(doc -> new Tuple<>(doc, this.getPriority(doc)))
        .forEach(tuple -> {
          try {
            newsCollection.updateOne(
                doc("_id", eq(tuple.getA().getObjectId("_id"))),
                set(doc("priority", tuple.getB())));
          } catch (Exception e) {
            log.error("Error Updating Priority", e);
          }
        });
  }

  private int getPriority(final Document doc) {
    // TODO determine
    return -1;
  }
}
