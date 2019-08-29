package com.keithmackay.api.auth;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.keithmackay.api.model.LoginModel;
import com.keithmackay.api.utils.Elective;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.keithmackay.api.db.Database.upsert;

public class AuthUtils {
  private final static Logger log = LoggerFactory.getLogger(AuthUtils.class);

  private static final char[] CHARS =
      "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890-_=+*$#@!`~".toCharArray();

  public static String randomToken() {
    return randomToken(128);
  }

  public static String randomToken(final int size) {
    return randomToken(128, ThreadLocalRandom.current());
  }

  public static String randomToken(final int size, final Random random) {
    final StringBuilder token = new StringBuilder();
    for (int i = 0; i < size; i++) {
      token.append(CHARS[random.nextInt(CHARS.length)]);
    }
    return token.toString();
  }

  public static String toHexString(final String s) {
    return String.format("%024x", new BigInteger(1, s.getBytes(/*YOUR_CHARSET?*/)));
  }

  private static Instant now() {
    return OffsetDateTime.now(ZoneOffset.UTC).toInstant();
  }

  private static Document createNewAuth(final String username) {
    log.info("Creating new Token for {}", username);
    return new Document("_id", new ObjectId(toHexString(username)))
        .append("username", username)
        .append("timeout", now()
            .plus(1, ChronoUnit.DAYS)
            .toEpochMilli())
        .append("timeLoggedIn", now().toEpochMilli())
        .append("token", randomToken());
  }

  public static Elective<Document> login(
      final MongoCollection<Document> tokenCollection,
      final MongoCollection<Document> userCollection,
      final LoginModel creds) {
    try {
      if (!checkCredentials(userCollection, creds)) {
        return Elective.empty();
      }
      final ObjectId id = new ObjectId(toHexString(creds.getUsername()));
      final Document doc = Elective.ofNullable(tokenCollection
          .find(new Document("_id", id)).limit(1).first())
          .map(d -> d.getLong("timeout") >
              now().toEpochMilli() ? d : null)
          .orElseGet(() -> {
            final Document created = createNewAuth(creds.getUsername());
            final UpdateResult result = upsert(tokenCollection, new Document("_id", id), created);
            return created;
          });
      return Elective.of(doc);
    } catch (Exception e) {
      log.error("Error logging in", e);
      return Elective.empty();
    }
  }

  private static boolean checkCredentials(
      final MongoCollection<Document> userCollection,
      final LoginModel creds) {
    if (Strings.isNullOrEmpty(creds.getUsername()) || Strings.isNullOrEmpty(creds.getPassword())) {
      log.warn("Both username and password are required");
      return false;
    }

    final Elective<Document> user = Elective.ofNullable(userCollection.find(new Document("username",
        new Document("$eq", creds.getUsername())))
        .limit(1).first());
    if (!user.isPresent()) {
      log.warn("Could not find user information for {}", creds.getUsername());
      return false;
    }

    if (!hashPass(creds.getPassword()).equals(user.get().getString("password"))) {
      log.warn("Passwords do not match for username '{}'", creds.getUsername());
      return false;
    }

    return true;
  }

  public static String hashPass(final String password) {
    return Hashing.sha512().hashString(password, StandardCharsets.UTF_8).toString();
  }

  public static String urlEncode(final String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
