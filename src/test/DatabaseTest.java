import com.keithmackay.api.ServerModule;
import com.keithmackay.api.db.Database;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseTest {

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = ServerModule.getInjector().getInstance(Database.class);
  }

  @Test
  void testSet() {
//    final DataSet collection = db.getCollection("name");
//    final ObjectId id = collection.upsert(new Document("hi", "keith"));
//    assertNotNull(id);
  }

  @Test
  void testGet() {
//    db.getCollection("test", ).upsert(new Document("hi", "keith"));
//    final Collection<Document> docs = db.getCollection("name").getAll();
//    Assertions.assertNotEmpty(docs, "There should have been one thing in the collection!");
//    final Document doc = Iterables.get(docs, 0);
//    assertNotNull(doc);
//    assertEquals("keith", doc.get("hi"));
  }
}
