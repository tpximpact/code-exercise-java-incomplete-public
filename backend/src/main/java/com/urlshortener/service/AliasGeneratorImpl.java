package com.urlshortener.service;

import org.springframework.stereotype.Component;

@Component
public class AliasGeneratorImpl implements AliasGenerator {

    private static final String ALIAS_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int GENERATED_ALIAS_LENGTH = 7;

    @Override
    public String generate() {
        // TODO: Implement alias generation.
        return "";
    }
}
