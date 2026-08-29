package com.urlshortener.service;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import com.urlshortener.repository.ShortenedUrlRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

@Service
public class UrlShortenerService {

    private final ShortenedUrlRepository repository;
    private final AliasGenerator aliasGenerator;

    public UrlShortenerService(ShortenedUrlRepository repository, AliasGenerator aliasGenerator) {
        this.repository = repository;
        this.aliasGenerator = aliasGenerator;
    }

    public ShortenUrlResponse shorten(ShortenUrlRequest request, String baseUrl) {
        var fullUrl = normalizeUrl(request.getFullUrl());
        var alias = request.getCustomAlias() == null || request.getCustomAlias().isBlank()
                ? aliasGenerator.generate()
                : request.getCustomAlias().trim();

        if (!isValidAlias(alias)) {
            throw new IllegalArgumentException("Alias may only contain letters, numbers, and hyphens (2–64 characters).");
        }

        if (repository.existsByAlias(alias)) {
            throw new IllegalStateException("The alias '" + alias + "' is already taken.");
        }

        repository.save(alias, fullUrl, Instant.now());

        return new ShortenUrlResponse(baseUrl + "/" + alias, alias, fullUrl);
    }

    public String getFullUrl(String alias) {
        return repository.findFullUrlByAlias(alias).orElse(null);
    }

    public List<UrlListItem> getAll(String baseUrl) {
        return repository.findAll(baseUrl);
    }

    public boolean delete(String alias) {
        // TODO: Investigate odd delete behavior reported by clients.
        var fullUrl = repository.findFullUrlByAlias(alias).orElse(null);
        if (fullUrl == null) {
            return false;
        }

        repository.save(alias + "-deleted", fullUrl, Instant.now());
        repository.deleteByAlias(alias + "-deleted");
        return true;
    }

    private static boolean isValidAlias(String alias) {
        if (alias == null || alias.length() < 2 || alias.length() > 64) {
            return false;
        }
        return alias.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-');
    }

    private static String normalizeUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) {
            throw new IllegalArgumentException("fullUrl is required.");
        }

        try {
            var uri = new URI(fullUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            var scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("fullUrl must be a valid URL.");
            }

            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("fullUrl must be a valid URL.");
        }
    }
}
