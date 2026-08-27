package com.inventario.multisucursal.suppliers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.multisucursal.users.RoleCode;
import com.inventario.multisucursal.users.User;
import com.inventario.multisucursal.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** RF-012: proveedores, ciclo mínimo. Lectura abierta; escritura OPERATOR/ADMIN (igual que products). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SupplierApiTest {

    private static final String SEED_PASSWORD = "ChangeMe123!";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String operatorToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
        userRepository.deleteAll();

        String hash = passwordEncoder.encode(SEED_PASSWORD);
        userRepository.save(new User("Admin", "admin@test.local", hash, RoleCode.ADMIN, null));

        operatorToken = login("admin@test.local");
        managerToken = operatorToken;
    }

    @Test
    void adminCanCreateSupplier() {
        ResponseEntity<SupplierResponse> response = post(
                "/api/v1/suppliers", Map.of("name", "Proveedor Uno", "taxId", "TAX-001"), operatorToken, SupplierResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("Proveedor Uno");
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    void creatingSupplierWithDuplicateTaxIdReturns409() {
        post("/api/v1/suppliers", Map.of("name", "Uno", "taxId", "TAX-DUP"), operatorToken, SupplierResponse.class);

        ResponseEntity<String> response = post(
                "/api/v1/suppliers", Map.of("name", "Dos", "taxId", "TAX-DUP"), operatorToken, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("\"code\":\"IDENTIFICACION_FISCAL_YA_EXISTE\"");
    }

    @Test
    void anyAuthenticatedRoleCanReadSuppliers() {
        post("/api/v1/suppliers", Map.of("name", "Proveedor Lectura", "taxId", "TAX-READ"), operatorToken, SupplierResponse.class);

        ResponseEntity<String> response = getWithToken("/api/v1/suppliers", managerToken, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("TAX-READ");
    }

    @Test
    void supplierCanBeDeactivatedAndReactivated() {
        ResponseEntity<SupplierResponse> created = post(
                "/api/v1/suppliers", Map.of("name", "Proveedor Baja", "taxId", "TAX-BAJA"), operatorToken, SupplierResponse.class);
        String id = created.getBody().id();

        ResponseEntity<SupplierResponse> deactivated = postAction("/api/v1/suppliers/" + id + "/deactivate", operatorToken, SupplierResponse.class);
        assertThat(deactivated.getBody().active()).isFalse();

        ResponseEntity<SupplierResponse> reactivated = postAction("/api/v1/suppliers/" + id + "/activate", operatorToken, SupplierResponse.class);
        assertThat(reactivated.getBody().active()).isTrue();
    }

    @Test
    void updatingSupplierDoesNotChangeTaxId() {
        ResponseEntity<SupplierResponse> created = post(
                "/api/v1/suppliers", Map.of("name", "Original", "taxId", "TAX-UPD"), operatorToken, SupplierResponse.class);
        String id = created.getBody().id();

        ResponseEntity<SupplierResponse> updated = patch(
                "/api/v1/suppliers/" + id, Map.of("name", "Renombrado", "contactName", "Juan"), operatorToken, SupplierResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("Renombrado");
        assertThat(updated.getBody().taxId()).isEqualTo("TAX-UPD");
    }

    @Test
    void operationsOnNonexistentSupplierReturn404() {
        assertThat(getWithToken("/api/v1/suppliers/999999", operatorToken, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers ----

    private String login(String email) {
        var body = new com.inventario.multisucursal.auth.LoginRequest(email, SEED_PASSWORD);
        ResponseEntity<com.inventario.multisucursal.auth.LoginResponse> response =
                restTemplate.postForEntity("/api/v1/auth/login", body, com.inventario.multisucursal.auth.LoginResponse.class);
        return response.getBody().accessToken();
    }

    private <T> ResponseEntity<T> post(String path, Object body, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), responseType);
    }

    private <T> ResponseEntity<T> postAction(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(null, authHeaders(token)), responseType);
    }

    private <T> ResponseEntity<T> patch(String path, Object requestBody, String token, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(restTemplate.getRootUri() + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            T body = responseType.equals(String.class)
                    ? responseType.cast(response.body())
                    : objectMapper.readValue(response.body(), responseType);
            return ResponseEntity.status(response.statusCode()).body(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> ResponseEntity<T> getWithToken(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), responseType);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
