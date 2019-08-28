package com.keithmackay.api.db;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.keithmackay.api.utils.Elective;
import com.keithmackay.api.utils.Utils;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DataSetImpl implements DataSet {
  private final Map<ObjectId, Document> map;
  private final DatabasePersister persister;

  @lombok.Getter
  private final String name;

  @Inject
  DataSetImpl(@Assisted String name,
              final DataSetInitializer initializer,
              final DatabasePersister persister) {
    this.map = new HashMap<>();
    this.name = name;
    this.persister = persister;
    this.map.putAll(initializer.initialize(name, DocumentCleaners.get(name)));
  }

  public void upsert(final ObjectId id, final Document value) {
    this.map.put(id, value.append("_id", id));
    this.persister.updated(this.map, this.getName());
  }

  public ObjectId upsert(final Document value) {
    final ObjectId id = new ObjectId();
    this.upsert(id, value);
    return id;
  }

  @Override
  public boolean hasId(ObjectId id) {
    return this.map.containsKey(id);
  }

  public Document getById(final ObjectId id) {
    return Utils.cloneDoc(this.map.get(id));
  }

  public Elective<Document> findById(final ObjectId id) {
    return Elective.ofNullable(this.getById(id));
  }

  public Collection<Document> getAll() {
    return this.map.values().stream()
        .map(Utils::cloneDoc)
        .collect(Collectors.toList());
  }
}
