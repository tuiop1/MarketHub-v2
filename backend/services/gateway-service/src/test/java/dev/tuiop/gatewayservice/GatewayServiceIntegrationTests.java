package dev.tuiop.gatewayservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import dev.tuiop.gatewayservice.ratelimiter.FixedWindowRateLimiter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://test-keycloak/realms/markethub",
        "app.rate-limit.request-limit=2",
        "app.rate-limit.window-size=1h",
        "management.tracing.export.enabled=false",
        "logging.structured.format.console=",
        "logging.level.root=WARN"
})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GatewayServiceIntegrationTests {

    private static final String ISSUER = "http://test-keycloak/realms/markethub";
    private static final RSAKey SIGNING_KEY = signingKey();
    private static final Upstream ACCOUNT = new Upstream("account-service");
    private static final Upstream CATALOG = new Upstream("catalog-service");
    private static final Upstream ORDER = new Upstream("order-service");
    private static final Upstream CART = new Upstream("cart-service");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> ACCOUNT.url() + "/jwks");
        for (Upstream upstream : List.of(ACCOUNT, CATALOG, ORDER, CART)) {
            registry.add("spring.cloud.discovery.client.simple.instances." + upstream.name + "[0].uri", upstream::url);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FixedWindowRateLimiter rateLimiter;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private String clientId;

    @BeforeEach
    void uniqueClient() {
        clientId = UUID.randomUUID().toString();
    }

    @AfterAll
    static void stopUpstreams() {
        List.of(ACCOUNT, CATALOG, ORDER, CART).forEach(upstream -> upstream.server.stop(0));
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/merchants, account-service, NONE",
            "GET, /api/v1/products, catalog-service, NONE",
            "GET, /api/v1/categories/coffee, catalog-service, NONE",
            "GET, /api/v1/orders, order-service, CUSTOMER",
            "GET, /api/v1/carts/current, cart-service, CUSTOMER",
            "POST, /api/v1/auth/customers/register, account-service, NONE",
            "POST, /api/v1/auth/merchants/register, account-service, NONE",
            "GET, /api/v1/admin/customers, account-service, ADMIN",
            "POST, /api/v1/admin/categories, catalog-service, ADMIN",
            "POST, /api/v1/merchant/products, catalog-service, MERCHANT"
    })
    void forwardsRoutesThroughServiceDiscovery(String method, String path, String upstream, String role) throws Exception {
        String bearer = role.equals("NONE") ? null : token(role);
        String body = method.equals("POST") ? "{\"name\":\"Coffee\"}" : "";
        HttpResponse<String> response = send(method, path + "?page=2", bearer, body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Upstream-Service")).contains(upstream);
        assertThat(response.headers().firstValue("X-Upstream-Method")).contains(method);
        assertThat(response.headers().firstValue("X-Upstream-Uri")).contains(path + "?page=2");
        assertThat(response.body()).isEqualTo(body.isEmpty() ? "{}" : body);
        if (bearer != null) {
            assertThat(response.headers().firstValue("X-Upstream-Authorization")).contains("Bearer " + bearer);
        }
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/customers/me, CUSTOMER",
            "/api/v1/merchants/me, MERCHANT_PENDING",
            "/api/v1/merchants/me, MERCHANT",
            "/api/v1/merchants/me, MERCHANT_REJECTED"
    })
    void acceptsTheExpectedProfileRoles(String path, String role) throws Exception {
        assertThat(send("GET", path, token(role), "").statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/customers/me, MERCHANT",
            "GET, /api/v1/merchants/me, CUSTOMER",
            "GET, /api/v1/admin/customers, CUSTOMER",
            "POST, /api/v1/merchant/products, MERCHANT_PENDING",
            "GET, /api/v1/orders, MERCHANT",
            "GET, /api/v1/carts, ADMIN"
    })
    void deniesTheWrongRoles(String method, String path, String role) throws Exception {
        assertThat(send(method, path, token(role), "").statusCode()).isEqualTo(403);
        assertThat(redisTemplate.keys("rate:" + clientId + ":*")).isEmpty();
    }

    @Test
    void deniesMissingInvalidExpiredAndWrongIssuerTokens() throws Exception {
        assertThat(send("GET", "/api/v1/orders", null, "").statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/orders", "invalid-token", "").statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/orders",
                token("CUSTOMER", ISSUER, Instant.now().minusSeconds(120), SIGNING_KEY), "").statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/orders",
                token("CUSTOMER", "http://wrong-issuer", Instant.now().plusSeconds(300), SIGNING_KEY), "").statusCode())
                .isEqualTo(401);
        assertThat(send("GET", "/api/v1/orders",
                token("CUSTOMER", ISSUER, Instant.now().plusSeconds(300), signingKey()), "").statusCode()).isEqualTo(401);
        assertThat(redisTemplate.keys("rate:" + clientId + ":*")).isEmpty();
    }

    @Test
    void blocksInternalEndpointsAndLeavesUnknownPathsUnrouted() throws Exception {
        for (String role : List.of("CUSTOMER", "MERCHANT", "ADMIN")) {
            assertThat(send("GET", "/internal/payments", token(role), "").statusCode()).isEqualTo(403);
        }
        assertThat(send("GET", "/api/v1/not-a-route", token("CUSTOMER"), "").statusCode()).isEqualTo(404);
        assertThat(redisTemplate.keys("rate:" + clientId + ":*")).isEmpty();
    }

    @Test
    void limitsRequestsAcrossRoutesAndReturnsTheExistingErrorFormat() throws Exception {
        assertThat(send("GET", "/api/v1/products", null, "").statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/merchants", null, "").statusCode()).isEqualTo(200);
        int calls = CATALOG.calls.get();

        HttpResponse<String> response = send("GET", "/api/v1/categories", null, "");

        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(response.headers().firstValue("Retry-After")).contains("3600");
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(value ->
                assertThat(value).startsWith("application/json"));
        var error = objectMapper.readTree(response.body());
        assertThat(error.get("status").asInt()).isEqualTo(429);
        assertThat(error.get("code").asText()).isEqualTo("Too Many Requests");
        assertThat(error.get("message").asText()).isEqualTo("Too many requests. Please try again later.");
        assertThat(error.get("path").asText()).isEqualTo("/api/v1/categories");
        assertThat(error.hasNonNull("timestamp")).isTrue();
        assertThat(CATALOG.calls.get()).isEqualTo(calls);

        Set<String> keys = redisTemplate.keys("rate:" + clientId + ":*");
        assertThat(keys).hasSize(1);
        assertThat(redisTemplate.getExpire(keys.iterator().next())).isBetween(1L, 3600L);
        clientId = UUID.randomUUID().toString();
        assertThat(send("GET", "/api/v1/categories", null, "").statusCode()).isEqualTo(200);
    }

    @Test
    void healthAndInfoRemainAccessibleAfterTheClientReachesItsLimit() throws Exception {
        send("GET", "/api/v1/products", null, "");
        send("GET", "/api/v1/products", null, "");
        assertThat(send("GET", "/actuator/health", null, "").statusCode()).isEqualTo(200);
        assertThat(send("GET", "/actuator/info", null, "").statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/products", null, "").statusCode()).isEqualTo(429);
    }

    @Test
    void preservesUpstreamErrorResponses() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/products/failure", null, "");
        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).isEqualTo("{\"message\":\"backend rejected\"}");
    }

    @Test
    void backendConnectionFailuresRemainServerErrorsOnPublicRoutes() throws Exception {
        assertThat(send("GET", "/api/v1/products/disconnect", null, "").statusCode()).isEqualTo(500);
    }

    @Test
    void usesTheRemoteAddressWhenTheForwardedIpIsMissing() throws Exception {
        clientId = null;
        assertThat(send("GET", "/api/v1/products", null, "").statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/products", null, "").statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/products", null, "").statusCode()).isEqualTo(429);
        assertThat(redisTemplate.keys("rate:127.0.0.1:*")).hasSize(1);
    }

    @Test
    void startsAFreshCounterInTheNextFixedWindow() throws Exception {
        Duration window = Duration.ofSeconds(1);
        assertThat(rateLimiter.allowRequest(clientId, 1, window)).isTrue();
        Set<String> previousKeys = redisTemplate.keys("rate:" + clientId + ":*");
        assertThat(previousKeys).hasSize(1);
        long previousWindow = Long.parseLong(previousKeys.iterator().next().substring(("rate:" + clientId + ":").length()));
        long nextWindowStart = (previousWindow + 1) * window.toMillis();
        Thread.sleep(Math.max(0, nextWindowStart - System.currentTimeMillis()) + 20);

        assertThat(rateLimiter.allowRequest(clientId, 1, window)).isTrue();
        assertThat(redisTemplate.keys("rate:" + clientId + ":*"))
                .anyMatch(key -> !previousKeys.contains(key));
        Thread.sleep(window.toMillis() + 50);
        assertThat(redisTemplate.keys("rate:" + clientId + ":*")).isEmpty();
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (clientId != null) {
            request.header("X-Real-IP", clientId);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String token(String role) throws Exception {
        return token(role, ISSUER, Instant.now().plusSeconds(300), SIGNING_KEY);
    }

    private static String token(String role, String issuer, Instant expiry, RSAKey signingKey) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(SIGNING_KEY.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .subject("test-user")
                        .issueTime(Date.from(Instant.now().minusSeconds(300)))
                        .expirationTime(Date.from(expiry))
                        .claim("realm_access", Map.of("roles", List.of(role)))
                        .build());
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static RSAKey signingKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID("test-key")
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class Upstream {
        private final String name;
        private final HttpServer server;
        private final AtomicInteger calls = new AtomicInteger();

        private Upstream(String name) {
            this.name = name;
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                server.createContext("/jwks", exchange -> {
                    byte[] keys = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, keys.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(keys);
                    }
                });
                server.createContext("/", exchange -> {
                    calls.incrementAndGet();
                    if (exchange.getRequestURI().getPath().endsWith("/disconnect")) {
                        exchange.close();
                        return;
                    }
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    var headers = exchange.getResponseHeaders();
                    headers.set("Content-Type", "application/json");
                    headers.set("X-Upstream-Service", name);
                    headers.set("X-Upstream-Method", exchange.getRequestMethod());
                    headers.set("X-Upstream-Uri", exchange.getRequestURI().toString());
                    String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                    if (authorization != null) {
                        headers.set("X-Upstream-Authorization", authorization);
                    }
                    boolean failure = exchange.getRequestURI().getPath().endsWith("/failure");
                    if (failure) {
                        body = "{\"message\":\"backend rejected\"}".getBytes(StandardCharsets.UTF_8);
                    } else if (body.length == 0) {
                        body = "{}".getBytes(StandardCharsets.UTF_8);
                    }
                    exchange.sendResponseHeaders(failure ? 422 : 200, body.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                });
                server.start();
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private String url() {
            return "http://localhost:" + server.getAddress().getPort();
        }
    }
}
