package com.keithmackay.api;

import com.google.inject.Guice;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args) {
        try {
            Guice.createInjector(new ServerModule()).getInstance(Server.class).start();
        } catch (Exception e) {
            LoggerFactory.getLogger(Main.class).error("Error Running TwitterScraper!", e);
        }
    }
}