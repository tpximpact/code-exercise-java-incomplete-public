package com.urlshortener;

import com.urlshortener.service.AliasGeneratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AliasGeneratorTests {

    private static final int ALIAS_LENGTH = 7;
    private static final String VALID_ALIAS_PATTERN = "[a-zA-Z0-9]+";

    private AliasGeneratorImpl aliasGenerator;

    @BeforeEach
    void setUp() {
        aliasGenerator = new AliasGeneratorImpl();
    }

    @Test
    void generate_ReturnsAliasWithValidLengthAndCharacters() {
        String alias = aliasGenerator.generate();

        assertThat(alias)
                .hasSize(ALIAS_LENGTH)
                .matches(VALID_ALIAS_PATTERN);
    }

    @Test
    void generate_ReturnsDifferentAliases() {
        String first = aliasGenerator.generate();
        String second = aliasGenerator.generate();

        assertThat(first).isNotEqualTo(second);
    }
}