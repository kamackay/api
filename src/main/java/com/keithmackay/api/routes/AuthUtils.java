package com.keithmackay.api.routes;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.keithmackay.api.db.DataSet;
import com.keithmackay.api.model.LoginModel;
import com.keithmackay.api.utils.Elective;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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
    return new Document("username", username)
        .append("timeout", now()
            .plus(1, ChronoUnit.DAYS)
            .toEpochMilli())
        .append("token", randomToken());
  }

  public static Elective<Document> login(final DataSet collection, final LoginModel creds) {
    try {
      if (Strings.isNullOrEmpty(creds.getUsername()) || Strings.isNullOrEmpty(creds.getPassword())) {
        // TODO VALIDATE PASSWORD
        return Elective.empty();
      }
      log.info("Attempt to log in with password {}", hashPass(creds.getPassword()));
      final ObjectId id = new ObjectId(toHexString(creds.getUsername()));
      final Document doc = collection
          .findById(id)
          .map(d -> d.getLong("timeout") >
              now().toEpochMilli() ? d : null)
          .orElseGet(() -> {
            final Document created = createNewAuth(creds.getUsername());
            collection.upsert(id, created);
            return created;
          });
      return Elective.of(doc);
    } catch (Exception e) {
      log.error("Error logging in", e);
      return Elective.empty();
    }
  }

  public static String hashPass(final String password) {
    return Hashing.sha512().hashString(password, StandardCharsets.UTF_8).toString();
  }
}
