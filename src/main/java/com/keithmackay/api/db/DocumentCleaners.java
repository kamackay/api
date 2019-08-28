package com.keithmackay.api.db;

import org.bson.Document;

import java.util.function.Function;

public class DocumentCleaners {
  public static Function<Document, Document> get(final String name) {
    switch (name) {
      case "auth":
        return DocumentCleaners::cleanAuth;
      default:
        return DocumentCleaners::dontClean;
    }
  }

  private static Document dontClean(final Document doc) {
    return doc;
  }

  private static Document cleanAuth(final Document doc) {
    try {
      return doc
          .append("timeout", doc.getDouble("timeout").longValue());
    } catch (Exception e) {
      // Assume the document has been cleaned, return the original
      return doc;
    }
  }
}
