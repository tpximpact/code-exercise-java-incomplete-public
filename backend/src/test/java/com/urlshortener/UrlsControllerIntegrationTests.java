package com.urlshortener;

import com.urlshortener.model.AllUrlsResponse;
import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.model.UrlListItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

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

    @Nested
    class PostShorten {

        @Test
        void validCustomAlias_Returns201WithShortenUrlResponse() {
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
        void badRequest_Returns400() {
            var req = new ShortenUrlRequest();
            req.setFullUrl("https://www.target.com/");
            req.setCustomAlias("invalid alias");

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    "/shorten",
                    HttpMethod.POST,
                    new HttpEntity<>(req),
                    new ParameterizedTypeReference<>() {}
            );

            assertEquals(BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    class GetAll {

        @Test
        void returns200WithSortedUrlListAndPagination() {
            insertUrl("old", "https://old.com/", "2026-08-31T10:00:00Z");
            insertUrl("middle", "https://middle.com/", "2026-08-31T11:00:00Z");
            insertUrl("new", "https://new.com/", "2026-08-31T12:00:00Z");

            ResponseEntity<AllUrlsResponse> response = restTemplate.exchange(
                    "/urls?page=0&size=2",
                    HttpMethod.GET,
                    null,
                    AllUrlsResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(OK);

            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.urls())
                    .extracting(UrlListItem::getAlias)
                    .containsExactly("new", "middle");

            assertThat(body.page()).isEqualTo(0);
            assertThat(body.size()).isEqualTo(2);
            assertThat(body.totalItems()).isEqualTo(3);
            assertThat(body.totalPages()).isEqualTo(2);
        }

        @Test
        void negativePage_Returns400() {
            var response = restTemplate.exchange(
                    "/urls?page=-1&size=5",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Page must be 0 or greater.");
        }

        @Test
        void invalidPageSize_Returns400() {
            var response = restTemplate.exchange(
                    "/urls?page=0&size=0",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Size must be at least 1.");
        }

        @Test
        void pageSizeExceedsMaximum_Returns400() {
            var response = restTemplate.exchange(
                    "/urls?page=0&size=101",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("error", "Size must not exceed 100.");
        }
    }

    @Nested
    class GetRedirectToUrl {

        @Test
        void existingAlias_Returns302WithLocation() {
            insertUrl("redir", "https://target.com/", "2026-08-31T14:00:00Z");

            var response = restTemplate.getForEntity("/redir", String.class);

            assertThat(response.getStatusCode()).isEqualTo(FOUND);
            assertThat(response.getHeaders().getLocation())
                    .hasToString("https://target.com/");
        }

        @Test
        void aliasNotFound_Returns404() {
            var response = restTemplate.getForEntity(
                    "/alias-does-not-exist",
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    class Delete {

        @Test
        void existingAlias_Returns204() {
            insertUrl("redir", "https://target.com/", "2026-08-31T14:00:00Z");

            var deleteResponse = restTemplate.exchange(
                    "/redir",
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            assertThat(deleteResponse.getStatusCode()).isEqualTo(NO_CONTENT);

            var getResponse = restTemplate.getForEntity(
                    "/redir",
                    String.class
            );

            assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        }

        @Test
        void aliasNotFound_Returns404() {
            var response = restTemplate.exchange(
                    "/alias-does-not-exist",
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        }
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