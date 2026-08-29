package com.inventario.multisucursal.suppliers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.multisucursal.branches.Branch;
import com.inventario.multisucursal.branches.BranchRepository;
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

/**
 * RF-012; BR-049: proveedores con CRUD completo abierto a cualquier rol
 * autenticado (a diferencia de {@code products}/{@code branches}, no hay
 * ningún subconjunto de roles con más capacidades que otro, ni restricción
 * por sucursal). Cubre creación válida, identificación fiscal duplicada,
 * lectura, edición sin alterar la identificación fiscal, activar/desactivar,
 * eliminación real sin datos asociados, recurso inexistente y que los tres
 * roles pueden ejercer cada escritura.
 *
 * <p>El caso "eliminar bloqueado por una orden de compra asociada" depende
 * íntegramente de la FK {@code ON DELETE RESTRICT} real de PostgreSQL —
 * Hibernate no la genera en el esquema de pruebas H2 porque el modelo no usa
 * asociaciones JPA (docs/DECISIONS.md) — y se verificó en vivo con `curl`
 * contra Docker Compose, igual que el caso análogo de {@code BranchApiTest}.
 */
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
    private BranchRepository branchRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String managerToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
        userRepository.deleteAll();
        branchRepository.deleteAll();

        Branch branch = branchRepository.save(new Branch("SUC-PROV", "Sucursal Proveedores"));
        String hash = passwordEncoder.encode(SEED_PASSWORD);
        userRepository.save(new User("Admin", "admin@test.local", hash, RoleCode.ADMIN, null));
        userRepository.save(new User("Gerente", "manager@test.local", hash, RoleCode.MANAGER, branch.getId()));
        userRepository.save(new User("Operador", "operator@test.local", hash, RoleCode.OPERATOR, branch.getId()));

        adminToken = login("admin@test.local");
        managerToken = login("manager@test.local");
        operatorToken = login("operator@test.local");
    }

    // ---- Creación válida y duplicados ----

    @Test
    void adminCanCreateSupplier() {
        ResponseEntity<SupplierResponse> response = post(
                "/api/v1/suppliers", Map.of("name", "Proveedor Uno", "taxId", "TAX-001"), adminToken, SupplierResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("Proveedor Uno");
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    void managerAndOperatorCanAlsoCreateSupplier() {
        assertThat(post("/api/v1/suppliers", Map.of("name", "Del gerente", "taxId", "TAX-MGR"), managerToken, SupplierResponse.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post("/api/v1/suppliers", Map.of("name", "Del operador", "taxId", "TAX-OPR"), operatorToken, SupplierResponse.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void creatingSupplierWithDuplicateTaxIdReturns409() {
        post("/api/v1/suppliers", Map.of("name", "Uno", "taxId", "TAX-DUP"), adminToken, SupplierResponse.class);

        ResponseEntity<String> response = post(
                "/api/v1/suppliers", Map.of("name", "Dos", "taxId", "TAX-DUP"), adminToken, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("\"code\":\"IDENTIFICACION_FISCAL_YA_EXISTE\"");
    }

    @Test
    void anyAuthenticatedRoleCanReadSuppliers() {
        post("/api/v1/suppliers", Map.of("name", "Proveedor Lectura", "taxId", "TAX-READ"), adminToken, SupplierResponse.class);

        ResponseEntity<String> response = getWithToken("/api/v1/suppliers", operatorToken, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("TAX-READ");
    }

    // ---- Edición y activación/desactivación por cualquier rol ----

    @Test
    void updatingSupplierDoesNotChangeTaxId() {
        ResponseEntity<SupplierResponse> created = post(
                "/api/v1/suppliers", Map.of("name", "Original", "taxId", "TAX-UPD"), adminToken, SupplierResponse.class);
        String id = created.getBody().id();

        ResponseEntity<SupplierResponse> updated = patch(
                "/api/v1/suppliers/" + id, Map.of("name", "Renombrado", "contactName", "Juan"), managerToken, SupplierResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("Renombrado");
        assertThat(updated.getBody().taxId()).isEqualTo("TAX-UPD");
    }

    @Test
    void supplierCanBeDeactivatedAndReactivatedByAnyRole() {
        ResponseEntity<SupplierResponse> created = post(
                "/api/v1/suppliers", Map.of("name", "Proveedor Baja", "taxId", "TAX-BAJA"), adminToken, SupplierResponse.class);
        String id = created.getBody().id();

        ResponseEntity<SupplierResponse> deactivated = postAction("/api/v1/suppliers/" + id + "/deactivate", operatorToken, SupplierResponse.class);
        assertThat(deactivated.getBody().active()).isFalse();

        ResponseEntity<SupplierResponse> reactivated = postAction("/api/v1/suppliers/" + id + "/activate", managerToken, SupplierResponse.class);
        assertThat(reactivated.getBody().active()).isTrue();
    }

    // ---- Eliminación real ----

    @Test
    void anyRoleCanDeleteSupplierWithNoAssociatedData() {
        ResponseEntity<SupplierResponse> created = post(
                "/api/v1/suppliers", Map.of("name", "Sin datos", "taxId", "TAX-VACIO"), adminToken, SupplierResponse.class);
        Long id = Long.valueOf(created.getBody().id());

        ResponseEntity<Void> response = delete("/api/v1/suppliers/" + id, operatorToken, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(supplierRepository.findById(id)).isEmpty();
    }

    // ---- Recurso inexistente ----

    @Test
    void operationsOnNonexistentSupplierReturn404() {
        long missingId = 999_999L;

        assertThat(getWithToken("/api/v1/suppliers/" + missingId, adminToken, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch("/api/v1/suppliers/" + missingId, Map.of("name", "X"), adminToken, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(postAction("/api/v1/suppliers/" + missingId + "/deactivate", adminToken, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(delete("/api/v1/suppliers/" + missingId, adminToken, String.class).getStatusCode())
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

    private <T> ResponseEntity<T> delete(String path, String token, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), responseType);
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
