package com.inventario.multisucursal.products;

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

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC de catálogo de productos (docs/USE_CASES.md; RF-005/RF-011). Cubre
 * exactamente lo pedido: CRUD válido, SKU duplicado, unidad de medida
 * inválida, factor de conversión inválido, permisos, y comportamiento de
 * producto inactivo.
 *
 * <p>{@code patch(...)} usa {@link HttpClient} (JDK) en vez de
 * {@code TestRestTemplate} por la misma limitación documentada en
 * {@code BranchApiTest}/{@code UserApiTest}: {@code HttpURLConnection} no
 * soporta el verbo PATCH.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ProductApiTest {

    private static final String SEED_PASSWORD = "ChangeMe123!";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnitOfMeasureRepository unitOfMeasureRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUnitRepository productUnitRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UnitOfMeasure unUnit;
    private UnitOfMeasure cajaUnit;
    private String adminToken;
    private String managerToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        productUnitRepository.deleteAll();
        productRepository.deleteAll();
        unitOfMeasureRepository.deleteAll();
        userRepository.deleteAll();
        branchRepository.deleteAll();

        Branch branchA = branchRepository.save(new Branch("SUC-A", "Sucursal A", "Calle 1"));

        String hash = passwordEncoder.encode(SEED_PASSWORD);
        userRepository.save(new User("Admin", "admin@test.local", hash, RoleCode.ADMIN, null));
        userRepository.save(new User("Gerente A", "manager@test.local", hash, RoleCode.MANAGER, branchA.getId()));
        userRepository.save(new User("Operador A", "operator@test.local", hash, RoleCode.OPERATOR, branchA.getId()));

        adminToken = login("admin@test.local");
        managerToken = login("manager@test.local");
        operatorToken = login("operator@test.local");

        unUnit = unitOfMeasureRepository.save(new UnitOfMeasure("UN", "Unidad"));
        cajaUnit = unitOfMeasureRepository.save(new UnitOfMeasure("CJ", "Caja"));
    }

    // ---- CRUD válido ----

    @Test
    void operatorCanCreateProductAndBaseUnitIsAutoCreated() {
        ResponseEntity<ProductResponse> response = post(
                "/api/v1/products",
                new CreateProductRequest("SKU-001", "Producto 1", "Descripción", unUnit.getId()),
                operatorToken,
                ProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().sku()).isEqualTo("SKU-001");
        assertThat(response.getBody().active()).isTrue();

        ResponseEntity<ProductUnitResponse[]> units = getWithToken(
                "/api/v1/products/" + response.getBody().id() + "/units", operatorToken, ProductUnitResponse[].class);
        assertThat(units.getBody()).hasSize(1);
        assertThat(units.getBody()[0].baseUnit()).isTrue();
        assertThat(units.getBody()[0].conversionFactorToBase()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void adminCanUpdateProductNameAndDescription() {
        String productId = createProduct("SKU-002", "Original", unUnit.getId());

        ResponseEntity<ProductResponse> response = patch(
                "/api/v1/products/" + productId,
                new UpdateProductRequest("Renombrado", "Nueva descripción"),
                adminToken,
                ProductResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Renombrado");
        assertThat(response.getBody().description()).isEqualTo("Nueva descripción");
        assertThat(response.getBody().sku()).isEqualTo("SKU-002");
    }

    @Test
    void anyAuthenticatedRoleCanReadProducts() {
        String productId = createProduct("SKU-003", "Producto 3", unUnit.getId());

        ResponseEntity<String> list = getWithToken("/api/v1/products", managerToken, String.class);
        ResponseEntity<ProductResponse> detail = getWithToken("/api/v1/products/" + productId, managerToken, ProductResponse.class);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().id()).isEqualTo(productId);
    }

    @Test
    void operatorCanAddAlternateUnitToProduct() {
        String productId = createProduct("SKU-004", "Producto 4", unUnit.getId());

        ResponseEntity<ProductUnitResponse> response = post(
                "/api/v1/products/" + productId + "/units",
                new AddProductUnitRequest(cajaUnit.getId(), new BigDecimal("12.000000")),
                operatorToken,
                ProductUnitResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().unitCode()).isEqualTo("CJ");
        assertThat(response.getBody().conversionFactorToBase()).isEqualByComparingTo(new BigDecimal("12.000000"));
        assertThat(response.getBody().baseUnit()).isFalse();
    }

    // ---- SKU duplicado ----

    @Test
    void creatingProductWithDuplicateSkuReturns409() {
        createProduct("SKU-DUP", "Primero", unUnit.getId());

        ResponseEntity<String> response = post(
                "/api/v1/products",
                new CreateProductRequest("SKU-DUP", "Segundo", null, unUnit.getId()),
                operatorToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("\"code\":\"SKU_YA_EXISTE\"");
    }

    // ---- Unidad de medida inválida ----

    @Test
    void creatingProductWithNonexistentUnitReturns404() {
        ResponseEntity<String> response = post(
                "/api/v1/products",
                new CreateProductRequest("SKU-005", "Producto 5", null, 999_999L),
                operatorToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"code\":\"UNIDAD_DE_MEDIDA_NO_ENCONTRADA\"");
    }

    @Test
    void addingNonexistentUnitToProductReturns404() {
        String productId = createProduct("SKU-006", "Producto 6", unUnit.getId());

        ResponseEntity<String> response = post(
                "/api/v1/products/" + productId + "/units",
                new AddProductUnitRequest(999_999L, BigDecimal.TEN),
                operatorToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"code\":\"UNIDAD_DE_MEDIDA_NO_ENCONTRADA\"");
    }

    @Test
    void addingAlreadyAssociatedUnitReturns409() {
        String productId = createProduct("SKU-007", "Producto 7", unUnit.getId());

        ResponseEntity<String> response = post(
                "/api/v1/products/" + productId + "/units",
                new AddProductUnitRequest(unUnit.getId(), BigDecimal.TEN),
                operatorToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("\"code\":\"UNIDAD_YA_ASOCIADA\"");
    }

    // ---- Conversión inválida ----

    @Test
    void addingUnitWithNonPositiveConversionFactorReturns400() {
        String productId = createProduct("SKU-008", "Producto 8", unUnit.getId());

        ResponseEntity<String> response = post(
                "/api/v1/products/" + productId + "/units",
                new AddProductUnitRequest(cajaUnit.getId(), BigDecimal.ZERO),
                operatorToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updatingBaseUnitConversionFactorReturns422() {
        String productId = createProduct("SKU-009", "Producto 9", unUnit.getId());

        ResponseEntity<String> response = patch(
                "/api/v1/products/" + productId + "/units/" + unUnit.getId(),
                new UpdateProductUnitConversionRequest(new BigDecimal("5.000000")),
                operatorToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("\"code\":\"UNIDAD_BASE_INMUTABLE\"");
    }

    @Test
    void updatingAlternateUnitConversionFactorSucceeds() {
        String productId = createProduct("SKU-010", "Producto 10", unUnit.getId());
        post("/api/v1/products/" + productId + "/units",
                new AddProductUnitRequest(cajaUnit.getId(), new BigDecimal("12.000000")),
                operatorToken, ProductUnitResponse.class);

        ResponseEntity<ProductUnitResponse> response = patch(
                "/api/v1/products/" + productId + "/units/" + cajaUnit.getId(),
                new UpdateProductUnitConversionRequest(new BigDecimal("24.000000")),
                operatorToken,
                ProductUnitResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().conversionFactorToBase()).isEqualByComparingTo(new BigDecimal("24.000000"));
    }

    // ---- Permisos ----

    @Test
    void managerCannotCreateProduct() {
        ResponseEntity<String> response = post(
                "/api/v1/products",
                new CreateProductRequest("SKU-011", "Producto 11", null, unUnit.getId()),
                managerToken,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("\"code\":\"ROL_NO_AUTORIZADO\"");
    }

    @Test
    void adminCanCreateUnitOfMeasureButOperatorCannot() {
        ResponseEntity<UnitOfMeasureResponse> adminResponse = post(
                "/api/v1/units-of-measure", new CreateUnitOfMeasureRequest("LT", "Litro"), adminToken, UnitOfMeasureResponse.class);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> operatorResponse = post(
                "/api/v1/units-of-measure", new CreateUnitOfMeasureRequest("KG2", "Kilogramo 2"), operatorToken, String.class);
        assertThat(operatorResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(operatorResponse.getBody()).contains("\"code\":\"ROL_NO_AUTORIZADO\"");
    }

    // ---- Producto inactivo ----

    @Test
    void deactivatedProductRemainsReadableAndCanBeReactivated() {
        String productId = createProduct("SKU-012", "Producto 12", unUnit.getId());

        ResponseEntity<ProductResponse> deactivated = postAction(
                "/api/v1/products/" + productId + "/deactivate", operatorToken, ProductResponse.class);
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody().active()).isFalse();

        ResponseEntity<ProductResponse> readAfterDeactivate = getWithToken(
                "/api/v1/products/" + productId, operatorToken, ProductResponse.class);
        assertThat(readAfterDeactivate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readAfterDeactivate.getBody().active()).isFalse();

        ResponseEntity<ProductResponse> reactivated = postAction(
                "/api/v1/products/" + productId + "/activate", operatorToken, ProductResponse.class);
        assertThat(reactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reactivated.getBody().active()).isTrue();
    }

    @Test
    void listSupportsActiveFilter() {
        String activeId = createProduct("SKU-013", "Activo", unUnit.getId());
        String inactiveId = createProduct("SKU-014", "Inactivo", unUnit.getId());
        postAction("/api/v1/products/" + inactiveId + "/deactivate", operatorToken, ProductResponse.class);

        ResponseEntity<String> onlyActive = getWithToken("/api/v1/products?active=true", operatorToken, String.class);

        assertThat(onlyActive.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(onlyActive.getBody()).contains("\"SKU-013\"");
        assertThat(onlyActive.getBody()).doesNotContain("\"SKU-014\"");
        assertThat(activeId).isNotEqualTo(inactiveId);
    }

    // ---- Recurso inexistente ----

    @Test
    void operationsOnNonexistentProductReturn404() {
        long missingId = 999_999L;

        assertThat(getWithToken("/api/v1/products/" + missingId, adminToken, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch("/api/v1/products/" + missingId, new UpdateProductRequest("X", null), adminToken, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(postAction("/api/v1/products/" + missingId + "/deactivate", adminToken, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers ----

    private String createProduct(String sku, String name, Long baseUnitId) {
        ResponseEntity<ProductResponse> response = post(
                "/api/v1/products", new CreateProductRequest(sku, name, null, baseUnitId), operatorToken, ProductResponse.class);
        return response.getBody().id();
    }

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
