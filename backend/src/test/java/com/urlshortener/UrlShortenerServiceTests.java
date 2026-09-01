package com.urlshortener;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.AliasGenerator;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlShortenerServiceTests {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String VALID_ALIAS = "example";
    private static final String ORIGINAL_URL = "https://example.com";
    private static final String SHORT_URL = BASE_URL + "/" + VALID_ALIAS;

    private UrlShortenerService service;
    @Mock
    private ShortenedUrlRepository repository;
    @Mock
    private AliasGenerator aliasGenerator;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(repository, aliasGenerator);
    }

    @Test
    void shorten_WithValidUrl_ReturnsShortUrlResponse() {
        when(repository.existsByAlias(VALID_ALIAS)).thenReturn(false);
        when(aliasGenerator.generate()).thenReturn(VALID_ALIAS);

        ShortenUrlResponse response = service.shorten(createShortenRequest(Optional.empty()), BASE_URL);

        verify(repository).save(
                eq(VALID_ALIAS),
                eq(ORIGINAL_URL),
                any(Instant.class)
        );

        assertEquals(VALID_ALIAS, response.getAlias());
        assertEquals(ORIGINAL_URL, response.getFullUrl());
        assertEquals(SHORT_URL, response.getShortUrl());
    }

    @Test
    void shorten_WithCustomAlias_ReturnsCustomAlias() {
        when(repository.existsByAlias(VALID_ALIAS)).thenReturn(false);

        ShortenUrlResponse response = service.shorten(createShortenRequest(Optional.of(VALID_ALIAS)), BASE_URL);

        verify(repository).save(
                eq(VALID_ALIAS),
                eq(ORIGINAL_URL),
                any(Instant.class)
        );

        verify(aliasGenerator, never()).generate();
        assertEquals(VALID_ALIAS, response.getAlias());
        assertEquals(SHORT_URL, response.getShortUrl());
    }

    @Test
    void shorten_WithTakenCustomAlias_ThrowsIllegalStateException() {
        when(repository.existsByAlias(VALID_ALIAS)).thenReturn(true);

        var request = createShortenRequest(Optional.of(VALID_ALIAS));

        var exception = assertThrows(
                IllegalStateException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals(
                "The alias '" + VALID_ALIAS + "' is already taken.",
                exception.getMessage()
        );

        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void shorten_WhenGeneratedAliasIsTaken_RetriesUntilAvailable() {
        when(aliasGenerator.generate())
                .thenReturn("taken1", "taken2", VALID_ALIAS);

        when(repository.existsByAlias("taken1")).thenReturn(true);
        when(repository.existsByAlias("taken2")).thenReturn(true);
        when(repository.existsByAlias(VALID_ALIAS)).thenReturn(false);

        ShortenUrlResponse response =
                service.shorten(createShortenRequest(Optional.empty()), BASE_URL);

        assertEquals(VALID_ALIAS, response.getAlias());

        verify(aliasGenerator, times(3)).generate();
        verify(repository).save(
                eq(VALID_ALIAS),
                eq(ORIGINAL_URL),
                any(Instant.class)
        );
    }

    @Test
    void shorten_WhenGeneratedAliasIsAlwaysTaken_ThrowsIllegalStateException() {
        when(aliasGenerator.generate())
                .thenReturn("taken1", "taken2", "taken3", "taken4", "taken5");

        when(repository.existsByAlias(any())).thenReturn(true);

        var exception = assertThrows(
                IllegalStateException.class,
                () -> service.shorten(createShortenRequest(Optional.empty()), BASE_URL)
        );

        assertEquals("Unable to generate a unique alias.", exception.getMessage());

        verify(aliasGenerator, times(5)).generate();
        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void shorten_WithInvalidCustomAlias_ThrowsIllegalArgumentException() {
        var request = createShortenRequest(Optional.of("invalid alias"));

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals(
                "Alias may only contain letters, numbers, and hyphens (2–64 characters).",
                exception.getMessage()
        );

        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void shorten_WithInvalidUrl_ThrowsIllegalArgumentException() {
        var request = new ShortenUrlRequest();
        request.setFullUrl("not-a-url");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals(
                "fullUrl must be a valid URL.",
                exception.getMessage()
        );

        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void shorten_WithMissingUrl_ThrowsIllegalArgumentException() {
        var request = new ShortenUrlRequest();

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.shorten(request, BASE_URL)
        );

        assertEquals("fullUrl is required.", exception.getMessage());

        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void getFullUrl_ReturnsExistingUrl() {
        when(repository.findFullUrlByAlias(VALID_ALIAS)).thenReturn(Optional.of(ORIGINAL_URL));

        String actual = service.getFullUrl(VALID_ALIAS);

        verify(repository).findFullUrlByAlias(VALID_ALIAS);
        assertEquals(ORIGINAL_URL, actual);
    }

    @Test
    void getFullUrl_WhenAliasDoesNotExist_ReturnsNull() {
        when(repository.findFullUrlByAlias(VALID_ALIAS))
                .thenReturn(Optional.empty());

        String actual = service.getFullUrl(VALID_ALIAS);

        verify(repository).findFullUrlByAlias(VALID_ALIAS);
        assertNull(actual);
    }

    private ShortenUrlRequest createShortenRequest(Optional<String> customAlias) {
        var request = new ShortenUrlRequest();
        request.setFullUrl(ORIGINAL_URL);
        customAlias.ifPresent(request::setCustomAlias);
        return request;
    }
}
