package com.healthcheck;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * Negative Test Cases - All error scenarios
 * Tests error handling across all API endpoints
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Negative Test Cases")
public class NegativeTest extends BaseIntegrationTest {

    private String generateUniqueEmail(String prefix) {
        return prefix + System.currentTimeMillis() + "@example.com";
    }

    // ========== HEALTH CHECK NEGATIVE TESTS ==========

    @Test
    @Order(1)
    @DisplayName("POST /healthz - Should return 405 Method Not Allowed")
    void testHealthCheck_PostNotAllowed() {
        given()
            .when()
                .post("/healthz")
            .then()
                .statusCode(405);
    }

    @Test
    @Order(2)
    @DisplayName("PUT /healthz - Should return 405 Method Not Allowed")
    void testHealthCheck_PutNotAllowed() {
        given()
            .when()
                .put("/healthz")
            .then()
                .statusCode(405);
    }

    @Test
    @Order(3)
    @DisplayName("DELETE /healthz - Should return 405 Method Not Allowed")
    void testHealthCheck_DeleteNotAllowed() {
        given()
            .when()
                .delete("/healthz")
            .then()
                .statusCode(405);
    }

    @Test
    @Order(4)
    @DisplayName("GET /health (wrong endpoint) - Should return 401 Unauthorized")
    void testWrongHealthEndpoint_NotFound() {
        given()
            .when()
                .get("/health")
            .then()
                .statusCode(401);
    }

    // ========== USER MANAGEMENT NEGATIVE TESTS ==========

    @Test
    @Order(10)
    @DisplayName("POST /v1/user - Missing required fields")
    void testCreateUser_MissingFields() {
        // Missing username
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "password": "Password123!",
                  "first_name": "Test",
                  "last_name": "User"
                }
                """)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(400);

        // Missing password
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "test@example.com",
                  "first_name": "Test",
                  "last_name": "User"
                }
                """)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(400);

        // Missing first_name
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "test2@example.com",
                  "password": "Password123!",
                  "last_name": "User"
                }
                """)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(11)
    @DisplayName("POST /v1/user - Invalid email format")
    void testCreateUser_InvalidEmailFormat() {
        String userJson = """
            {
              "username": "invalid-email-format",
              "password": "Password123!",
              "first_name": "Test",
              "last_name": "User"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    @DisplayName("POST /v1/user - Duplicate email")
    void testCreateUser_DuplicateEmail() {
        String email = generateUniqueEmail("duplicate");
        String userJson = """
            {
              "username": "%s",
              "password": "Password123!",
              "first_name": "Duplicate",
              "last_name": "Test"
            }
            """.formatted(email);

        // Create first user
        given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Try to create duplicate
        given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(13)
    @DisplayName("GET /v1/user/{id} - Non-existent user")
    void testGetUser_NotFound() {
        String email = generateUniqueEmail("getnotfound");
        String password = "NotFoundPass123!";
        
        // Create user for authentication
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "NotFound",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Try to get non-existent user
        given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user/99999")
            .then()
                .statusCode(anyOf(equalTo(404), equalTo(403)));
    }

    @Test
    @Order(14)
    @DisplayName("GET /v1/user/{id} - No authentication")
    void testGetUser_NoAuth() {
        given()
            .when()
                .get("/v1/user/1")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(15)
    @DisplayName("GET /v1/user/{id} - Invalid credentials")
    void testGetUser_InvalidAuth() {
        given()
            .auth().basic("nonexistent@example.com", "wrongpassword")
            .when()
                .get("/v1/user/1")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(16)
    @DisplayName("PUT /v1/user/{id} - Non-existent user")
    void testUpdateUser_NotFound() {
        String email = generateUniqueEmail("updatenotfound");
        String password = "UpdateNotFoundPass123!";
        
        // Create user for authentication
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "UpdateNotFound",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Try to update non-existent user
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "first_name": "Updated",
                  "last_name": "User",
                  "password": "NewPass123!"
                }
                """)
            .when()
                .put("/v1/user/99999")
            .then()
                .statusCode(anyOf(equalTo(404), equalTo(403)));
    }

    @Test
    @Order(17)
    @DisplayName("PUT /v1/user/{id} - Invalid JSON format")
    void testUpdateUser_InvalidJSON() {
        String email = generateUniqueEmail("invalidjson");
        String password = "InvalidJsonPass123!";
        
        // Create user
        String userId = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "InvalidJson",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Try to update with invalid JSON
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("{ invalid json format")
            .when()
                .put("/v1/user/" + userId)
            .then()
                .statusCode(400);
    }

    @Test
    @Order(18)
    @DisplayName("GET /v1/user - Unsupported endpoint without ID")
    void testGetAllUsers_NotSupported() {
        String email = generateUniqueEmail("getallusers");
        String password = "GetAllUsersPass123!";
    
        // Create user for authentication
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                "username": "%s",
                "password": "%s",
                "first_name": "GetAll",
                "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Test unsupported endpoint
        given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user")  // 沒有 ID
            .then()
                .statusCode(anyOf(equalTo(404), equalTo(405))); // 可能是 Not Found 或 Method Not Allowed
    }

    // ========== PRODUCT MANAGEMENT NEGATIVE TESTS ==========

    @Test
    @Order(20)
    @DisplayName("POST /v1/product - No authentication")
    void testCreateProduct_NoAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Test Product",
                  "description": "A test product",
                  "sku": "TEST-001",
                  "manufacturer": "Test Manufacturer",
                  "quantity": 10
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(21)
    @DisplayName("POST /v1/product - Missing required fields")
    void testCreateProduct_MissingFields() {
        String email = generateUniqueEmail("productmissing");
        String password = "ProductMissingPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                "username": "%s",
                "password": "%s",
                "first_name": "Product",
                "last_name": "Missing"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Missing name (required)
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "description": "A test product",
                "manufacturer": "Test Manufacturer",
                "quantity": 10
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(400);

        // Missing description (required)
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "name": "Test Product",
                "manufacturer": "Test Manufacturer",
                "quantity": 10
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(400);

        // Missing manufacturer (required)
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "name": "Test Product",
                "description": "A test product",
                "quantity": 10
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(400);

        // Missing quantity (required)
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "name": "Test Product",
                "description": "A test product",
                "manufacturer": "Test Manufacturer"
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(400);
    }
    
    @Test
    @Order(22)
    @DisplayName("POST /v1/product - Invalid quantity")
    void testCreateProduct_InvalidQuantity() {
        String email = generateUniqueEmail("invalidquantity");
        String password = "InvalidQuantityPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Invalid",
                  "last_name": "Quantity"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Negative quantity
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Invalid Product",
                  "description": "Product with negative quantity",
                  "sku": "INVALID-001",
                  "manufacturer": "Test Manufacturer",
                  "quantity": -5
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(23)
    @DisplayName("POST /v1/product - Duplicate SKU")
    void testCreateProduct_DuplicateSKU() {
        String email = generateUniqueEmail("duplicatesku");
        String password = "DuplicateSKUPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Duplicate",
                  "last_name": "SKU"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        String productJson = """
            {
              "name": "Duplicate SKU Product",
              "description": "Product with duplicate SKU",
              "sku": "DUPLICATE-SKU-001",
              "manufacturer": "Test Manufacturer",
              "quantity": 1
            }
            """;

        // Create first product
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body(productJson)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201);

        // Try to create product with same SKU
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body(productJson)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(400);
    }

    @Test
    @Order(24)
    @DisplayName("GET /v1/product/{id} - Non-existent product")
    void testGetProduct_NotFound() {
        given()
            .when()
                .get("/v1/product/99999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(25)
    @DisplayName("PUT /v1/product/{id} - Non-owner cannot update")
    void testUpdateProduct_NonOwner() {
        String ownerEmail = generateUniqueEmail("owner");
        String ownerPassword = "OwnerPass123!";
        String nonOwnerEmail = generateUniqueEmail("nonowner");
        String nonOwnerPassword = "NonOwnerPass123!";
        
        // Create owner
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Owner",
                  "last_name": "User"
                }
                """.formatted(ownerEmail, ownerPassword))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create non-owner
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "NonOwner",
                  "last_name": "User"
                }
                """.formatted(nonOwnerEmail, nonOwnerPassword))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create product as owner
        String productId = given()
            .auth().basic(ownerEmail, ownerPassword)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Owner Product",
                  "description": "Product owned by owner",
                  "sku": "OWNER-001",
                  "manufacturer": "Owner Manufacturer",
                  "quantity": 5
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Try to update as non-owner
        given()
            .auth().basic(nonOwnerEmail, nonOwnerPassword)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Unauthorized Update",
                  "description": "This should not be allowed",
                  "sku": "HACK-001",
                  "manufacturer": "Hacker Inc",
                  "quantity": 100
                }
                """)
            .when()
                .put("/v1/product/" + productId)
            .then()
                .statusCode(403);
    }

    @Test
    @Order(26)
    @DisplayName("PUT /v1/product/{id} - Non-existent product")
    void testUpdateProduct_NotFound() {
        String email = generateUniqueEmail("updateproductnotfound");
        String password = "UpdateProductNotFoundPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "UpdateProduct",
                  "last_name": "NotFound"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Try to update non-existent product
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Updated Product",
                  "description": "Updated description",
                  "sku": "UPDATED-001",
                  "manufacturer": "Updated Manufacturer",
                  "quantity": 25
                }
                """)
            .when()
                .put("/v1/product/99999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(27)
    @DisplayName("DELETE /v1/product/{id} - Non-owner cannot delete")
    void testDeleteProduct_NonOwner() {
        String ownerEmail = generateUniqueEmail("deleteowner");
        String ownerPassword = "DeleteOwnerPass123!";
        String nonOwnerEmail = generateUniqueEmail("deletenonowner");
        String nonOwnerPassword = "DeleteNonOwnerPass123!";
        
        // Create owner
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Delete",
                  "last_name": "Owner"
                }
                """.formatted(ownerEmail, ownerPassword))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create non-owner
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Delete",
                  "last_name": "NonOwner"
                }
                """.formatted(nonOwnerEmail, nonOwnerPassword))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create product as owner
        String productId = given()
            .auth().basic(ownerEmail, ownerPassword)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Delete Owner Product",
                  "description": "Product to be protected from deletion",
                  "sku": "DELETE-OWNER-001",
                  "manufacturer": "Delete Owner Manufacturer",
                  "quantity": 3
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Try to delete as non-owner
        given()
            .auth().basic(nonOwnerEmail, nonOwnerPassword)
            .when()
                .delete("/v1/product/" + productId)
            .then()
                .statusCode(403);
    }

    @Test
    @Order(28)
    @DisplayName("DELETE /v1/product/{id} - Non-existent product")
    void testDeleteProduct_NotFound() {
        String email = generateUniqueEmail("deletenotfound");
        String password = "DeleteNotFoundPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Delete",
                  "last_name": "NotFound"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Try to delete non-existent product
        given()
            .auth().basic(email, password)
            .when()
                .delete("/v1/product/99999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(29)
    @DisplayName("POST /v1/product/{id} - Wrong HTTP method")
    void testProduct_WrongMethod_POST() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/v1/product/99999")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(405), equalTo(404)));
    }

    @Test
    @Order(30)
    @DisplayName("DELETE /v1/user/{id} - Wrong HTTP method")
    void testUser_WrongMethod_DELETE() {
        given()
        .when()
            .delete("/v1/user/1")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(405)));
    }

    @Test
    @Order(31)
    @DisplayName("GET /v1/nonexistent - Unsupported endpoint")
    void testUnsupportedEndpoint() {
        given()
            .when()
                .get("/v1/nonexistent")
            .then()
                .statusCode(anyOf(equalTo(401), equalTo(404)));  // 或 401 如果被 Security 攔截
    }
}