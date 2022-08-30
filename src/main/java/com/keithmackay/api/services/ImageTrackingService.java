package com.keithmackay.api.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.email.EmailSender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static domn8.util.Utils.listOf;

@Slf4j
@Singleton
public class ImageTrackingService {

    private final EmailSender emailSender;

    private static final Map<String, TrackerConfig> configs = new HashMap<>() {{
        put("spadaforte", TrackerConfig.builder()
                .name("Spadaforte")
                .recipients(listOf("tracker@keith.sh"))
                .build());
    }};

    @Inject
    ImageTrackingService(final EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void triggerTrackerId(final String id, final List<String> additional) {
        final List<String> recipients = new ArrayList<>(additional);
        if (!configs.containsKey(id)) {
            emailSender.send(String.format("Unconfigured Tracker Image `%s` Loaded", id),
                    String.format("The Tracker Image with no config was loaded, which means the email it was in was opened! I am notifying the following addresses of this: %s",
                            String.join(", ", recipients)),
                    recipients);
            return;
        }

        final TrackerConfig config = configs.get(id);
        recipients.addAll(config.recipients);
        emailSender.send(String.format("Tracker Image `%s` Loaded", config.name),
                String.format("The Tracker Image with the name `%s` was loaded, which means the email it was in was opened! I am notifying the following addresses of this: %s",
                        config.name, String.join(", ", recipients)),
                recipients);
    }

    @Value
    @Builder
    @AllArgsConstructor
    public static class TrackerConfig {
        String name;
        List<String> recipients;
    }
}
