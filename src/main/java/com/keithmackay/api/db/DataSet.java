package com.keithmackay.api.db;

import com.keithmackay.api.utils.Elective;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Collection;

public interface DataSet {
  public void upsert(final ObjectId id, final Document value);

  public ObjectId upsert(final Document value);

  public boolean hasId(final ObjectId id);

  public Document getById(final ObjectId id);

  public Elective<Document> findById(final ObjectId id);

  public Collection<Document> getAll();
}
