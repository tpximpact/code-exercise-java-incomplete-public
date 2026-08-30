package com.urlshortener.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class AliasGeneratorImpl implements AliasGenerator {

    private static final String ALIAS_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int GENERATED_ALIAS_LENGTH = 7;

    public String generate() {
        var random = ThreadLocalRandom.current();
        var alias = new StringBuilder(GENERATED_ALIAS_LENGTH);

        for (int i = 0; i < GENERATED_ALIAS_LENGTH; i++) {
            alias.append(ALIAS_CHARS.charAt(
                    random.nextInt(ALIAS_CHARS.length())
            ));
        }

        return alias.toString();
    }
}
