package com.keithmackay.api.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.model.CryptoLookupBean;
import com.keithmackay.api.utils.JsonOptional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static com.keithmackay.api.utils.JavaUtils.streamIterable;

@Singleton
public class CryptoService {
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final OkHttpClient client;

  @Inject
  CryptoService() {
    this.client = new OkHttpClient.Builder()
        .build();
  }

  public void getAccounts(final CryptoLookupBean lookupBean) {
    try {
      final String url = "https://api.coinbase.com/v2/accounts";
      final long timestamp = System.currentTimeMillis() / 1000;
      Request.Builder request = new Request.Builder()
          .url(url);
      request.addHeader("CB-ACCESS-KEY", lookupBean.getKey());
      request.addHeader("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp));
      request.addHeader("CB-ACCESS-SIGN", encode(lookupBean.getSecret(),
          String.format("%dGET%s",
              timestamp, "/v2/accounts")));

      try (final Response response = client.newCall(request.build())
          .execute()) {
        final String json = Optional.of(response)
            .map(Response::body)
            .map(responseBody -> {
              try {
                return responseBody.string();
              } catch (IOException e) {
                log.error("Could not get Body", e);
                return null;
              }
            })
            .orElse("{}");
        log.debug(json);
        final JsonElement node = new JsonParser().parse(json);
        final JsonArray data = JsonOptional.of(node)
            .map(JsonElement::getAsJsonObject)
            .map(o -> o.get("data"))
            .map(JsonElement::getAsJsonArray)
            .get();
        
        streamIterable(data)
            .forEach(coin -> {
              try {
                final Double amount = JsonOptional.of(coin)
                    .map(JsonElement::getAsJsonObject)
                    .map(el -> el.get("balance"))
                    .map(JsonElement::getAsJsonObject)
                    .map(el -> el.get("amount"))
                    .mapOut(JsonElement::getAsDouble)
                    .orElse(0D);
                if (amount == 0) {
                  return;
                }

                final String name = JsonOptional.of(coin)
                    .map(JsonElement::getAsJsonObject)
                    .map(el -> el.get("currency"))
                    .map(JsonElement::getAsJsonObject)
                    .map(el -> el.get("name"))
                    .map(JsonElement::getAsJsonPrimitive)
                    .mapOut(JsonPrimitive::getAsString)
                    .orElse("Something");

                log.info("You have {} of {}", amount, name);
              } catch (Exception e) {
                log.error("Error Handling", e);
              }
            });

      }
    } catch (Exception e) {
      log.error("Error Getting Accounts From Coinbase", e);
    }
  }

  public static String encode(String key, String data) throws Exception {
    Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
    SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
    sha256_HMAC.init(secret_key);

    return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
  }

}
