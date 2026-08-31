package com.urlshortener;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC"
})
class UrlsControllerIntegrationTests {

    private static final String LOCAL_HOST = "http://localhost:";

    @LocalServerPort
    private int PORT;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM shortened_urls");
    }

    @Test
    void postShorten_ValidCustomAlias_Returns201WithShortenUrlResponse() {
        var req = new ShortenUrlRequest();
        req.setFullUrl("https://www.target.com/");
        req.setCustomAlias("redir");

        ResponseEntity<ShortenUrlResponse> response = restTemplate.exchange(
                "/shorten",
                HttpMethod.POST,
                new HttpEntity<>(req),
                ShortenUrlResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("redir", response.getBody().getAlias());
        assertEquals(
                LOCAL_HOST + PORT + "/redir",
                response.getBody().getShortUrl()
        );
    }

    @Test
    void postShorten_BadRequest_Returns400() {
        var req = new ShortenUrlRequest();
        req.setFullUrl("https://www.target.com/");
        req.setCustomAlias("invalid alias");

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "/shorten",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getAll_Returns200WithSortedUrlList() {
        insertUrl("old", "https://old.com/", "2026-08-31T10:00:00Z");
        insertUrl("middle", "https://middle.com/", "2026-08-31T11:00:00Z");
        insertUrl("new", "https://new.com/", "2026-08-31T12:00:00Z");

        ResponseEntity<List<UrlListItem>> response = restTemplate.exchange(
                "/urls",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(UrlListItem::getAlias)
                .containsExactly("new", "middle", "old");
    }

    @Test
    void getRedirectToUrl_ExistingAlias_Returns302WithLocation() {
        insertUrl("redir", "https://target.com/", "2026-08-31T14:00:00Z");

        var response = restTemplate.getForEntity("/redir", String.class);

        assertThat(response.getStatusCode()).isEqualTo(FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://target.com/");
    }

    @Test
    void getRedirectToUrl_AliasNotFound_Returns404() {
        var response = restTemplate.getForEntity("/alias-does-not-exist", String.class);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_ExistingAlias_Returns204() {
        insertUrl("redir", "https://target.com/", "2026-08-31T14:00:00Z");

        var deleteResponse = restTemplate.exchange(
                "/redir",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(NO_CONTENT);

        var getResponse = restTemplate.getForEntity("/redir", String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_AliasNotFound_Returns404() {
        var response = restTemplate.exchange(
                "/alias-does-not-exist",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    private void insertUrl(String alias, String fullUrl, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO shortened_urls (alias, full_url, created_at) VALUES (?, ?, ?)",
                alias,
                fullUrl,
                createdAt
        );
    }
}
