package com.urlshortener.model;

import java.util.List;

public record AllUrlsResponse(
        List<UrlListItem> urls,
        int page,
        int size,
        int totalItems,
        int totalPages
) {}