package com.keithmackay.api.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.model.CoinHolding;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.keithmackay.api.utils.JavaUtils.*;
import static java.util.Collections.synchronizedList;

@Singleton
public class CryptoService {
    private static final String BASE_URL = "https://api.coinbase.com";

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final OkHttpClient client;

    @Inject
    CryptoService() {
        this.client = new OkHttpClient.Builder()
                .build();
    }

    private static Request.Builder buildRequest(final CryptoLookupBean lookupBean,
                                                final String path) throws Exception {
        final long timestamp = System.currentTimeMillis() / 1000;
        Request.Builder request = new Request.Builder()
                .url(BASE_URL + path);
        request.addHeader("CB-ACCESS-KEY", lookupBean.getKey());
        request.addHeader("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp));
        request.addHeader("CB-ACCESS-SIGN", encode(lookupBean.getSecret(),
                String.format("%dGET%s",
                        timestamp, path)));

        return request;
    }

    public static String encode(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public List<CoinHolding> getAccounts(final CryptoLookupBean lookupBean) {
        final List<CoinHolding> holdings = synchronizedList(new ArrayList<>());
        try {
            final String url = "/v2/accounts";
            final Request.Builder request = buildRequest(lookupBean, url);

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
                final JsonElement node = JsonParser.parseString(json);
                final JsonArray data = JsonOptional.of(node)
                        .map(JsonElement::getAsJsonObject)
                        .map(o -> o.get("data"))
                        .map(JsonElement::getAsJsonArray)
                        .get();

                streamIterable(data)
                        .forEach(coin -> {
                            try {
                                final double amount = getDeepDouble(coin, "balance.amount")
                                        .orElse(0D);

                                if (amount == 0) {
                                    return;
                                }

                                final String name = getDeepString(coin, "currency.name")
                                        .orElse("Something");
                                final String code = getDeepString(coin, "currency.code")
                                        .orElse("");
                                final String color = getDeepString(coin, "currency.color")
                                        .orElse("#000");

                                this.valueInUSD(lookupBean, code)
                                        .ifPresent(value -> {
                                            holdings.add(new CoinHolding(
                                                    name,
                                                    code,
                                                    color,
                                                    amount,
                                                    value,
                                                    System.currentTimeMillis()));
                                        });
                            } catch (Exception e) {
                                log.error("Error Handling", e);
                            }
                        });

            }
        } catch (Exception e) {
            log.error("Error Getting Accounts From Coinbase", e);
        }
        return holdings;
    }

    public Optional<Double> valueInUSD(final CryptoLookupBean lookupBean, final String name) {
        try {
            final Request.Builder request = buildRequest(lookupBean,
                    String.format("/v2/prices/%s-USD/sell", name));

            try (final Response response = client.newCall(request.build())
                    .execute()) {
                final String json = response.body().string();
                log.debug(json);
                final JsonElement el = JsonParser.parseString(json);
                return getDeepDouble(el, "data.amount");
            }
        } catch (Exception e) {
            log.error("Could not lookup {}", name, e);
        }
        return Optional.empty();
    }

}
