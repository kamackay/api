package com.keithmackay.api.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.model.CryptoLookupBean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
             timestamp, url)));

     try (final Response response = client.newCall(request.build())
         .execute()) {
       log.info(response.body().string());
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
