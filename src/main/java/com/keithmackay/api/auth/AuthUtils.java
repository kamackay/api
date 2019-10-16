package com.keithmackay.api.auth;

import com.google.common.base.Strings;
import com.keithmackay.api.model.LoginModel;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.keithmackay.api.ConstantsKt.tokenTimeoutDays;
import static com.keithmackay.api.utils.UtilsKt.*;

public class AuthUtils {
  private final static Logger log = getLogger(AuthUtils.class);

  private static final int BCRYPT_ROUNDS = 4;

  private static final char[] CHARS =
      "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890-_=+*$#@!`~/\\".toCharArray();

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

  private static Document createNewToken(final String username) {
    final Instant now = now();
    log.info("Creating new Token for {}", username);
    return doc("username", username)
        .append("timeout", now
            .plus(tokenTimeoutDays(), ChronoUnit.DAYS)
            .toEpochMilli())
        .append("timeLoggedIn", now.toEpochMilli())
        .append("timeLoggedInReadable", now.toString())
        .append("token", randomToken());
  }

  public static Optional<Document> login(
      final MongoCollection<Document> userCollection,
      final MongoCollection<Document> tokenCollection,
      final LoginModel creds) {
    try {
      if (Strings.isNullOrEmpty(creds.getUsername()) || Strings.isNullOrEmpty(creds.getPassword())) {
        return Optional.empty();
      }
      final Document usernameFilter = doc("username", creds.getUsername());
      final Optional<Document> userMaybe = Optional.ofNullable(
          userCollection
              .find(usernameFilter)
              .first());
      if (userMaybe.isEmpty()) {
        return Optional.empty();
      } else {
        final Document user = userMaybe.get();
        if (checkPass(creds.getPassword(), user.getString("password"))) {
          // Passwords Matched, Generate Token and Return
          final Optional<Document> tokenMaybe = Optional.ofNullable(
              tokenCollection.find(usernameFilter).first());
          if (tokenMaybe.isPresent() && tokenMaybe
              .map(doc -> doc.getLong("timeout"))
              .orElse(0L) > now().toEpochMilli()) {
            // Valid Token Already exists, return it
            log.info("Sending existing token to {}", creds.getUsername());
            return tokenMaybe;
          } else {
            // Generate New token and return it
            final Document newToken = createNewToken(creds.getUsername());
            tokenCollection.updateOne(usernameFilter, set(newToken), upsert());
            return Optional.of(newToken);
          }
        } else {
          return Optional.empty();
        }
      }
    } catch (Exception e) {
      log.error("Error logging in", e);
      return Optional.empty();
    }
  }

  public static void logout(final MongoCollection<Document> tokenCollection, final String username) {
    final DeleteResult result = tokenCollection.deleteOne(doc("username", username));
    if (result.getDeletedCount() > 0) {
      log.info("Successfully Logged out {}", username);
    }
  }

  public static boolean checkPass(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }

  public static String hashPass(final String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
  }
}
