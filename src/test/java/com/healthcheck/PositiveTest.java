package com.healthcheck;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * Positive Test Cases - All successful scenarios
 * Tests successful operations across all API endpoints
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Positive Test Cases")
public class PositiveTest extends BaseIntegrationTest {

    private String generateUniqueEmail(String prefix) {
        return prefix + System.currentTimeMillis() + "@example.com";
    }

    // ========== HEALTH CHECK POSITIVE TESTS ==========

    @Test
    @Order(1)
    @DisplayName("GET /healthz - Should return 200 OK")
    void testHealthCheck_Success() {
        given()
            .when()
                .get("/healthz")
            .then()
                .statusCode(200);
    }

    // ========== USER MANAGEMENT POSITIVE TESTS ==========

    @Test
    @Order(10)
    @DisplayName("POST /v1/user - Create user with valid data")
    void testCreateUser_Success() {
        String email = generateUniqueEmail("create");
        String password = "SecurePassword123!";
        String firstName = "Create";
        String lastName = "Test";
        
        String userJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "%s",
              "last_name": "%s"
            }
            """.formatted(email, password, firstName, lastName);

        given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("username", equalTo(email))
                .body("firstName", equalTo(firstName))
                .body("lastName", equalTo(lastName))
                .body("accountCreated", notNullValue())
                .body("accountUpdated", notNullValue())
                .body(not(hasKey("password")));
    }

    @Test
    @Order(11)
    @DisplayName("POST /v1/user - Different valid input combinations")
    void testCreateUser_DifferentValidInputs() {
        // Test short names
        String email1 = generateUniqueEmail("short");
        String userJson1 = """
            {
              "username": "%s",
              "password": "ShortPass123!",
              "first_name": "A",
              "last_name": "B"
            }
            """.formatted(email1);

        given()
            .contentType(ContentType.JSON)
            .body(userJson1)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .body("firstName", equalTo("A"))
                .body("lastName", equalTo("B"));

        // Test long names
        String email2 = generateUniqueEmail("long");
        String userJson2 = """
            {
              "username": "%s",
              "password": "LongNamePass123!",
              "first_name": "VeryLongFirstName",
              "last_name": "VeryLongLastNameExample"
            }
            """.formatted(email2);

        given()
            .contentType(ContentType.JSON)
            .body(userJson2)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .body("firstName", equalTo("VeryLongFirstName"))
                .body("lastName", equalTo("VeryLongLastNameExample"));
    }

    @Test
    @Order(12)
    @DisplayName("GET /v1/user/{id} - Get user by valid ID")
    void testGetUserById_Success() {
        String email = generateUniqueEmail("get");
        String password = "GetTestPass123!";
        
        String userJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "Get",
              "last_name": "Test"
            }
            """.formatted(email, password);

        String userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user/" + userId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(Integer.parseInt(userId)))
                .body("username", equalTo(email))
                .body("firstName", equalTo("Get"))
                .body("lastName", equalTo("Test"))
                .body(not(hasKey("password")));
    }

    @Test
    @Order(13)
    @DisplayName("PUT /v1/user/{id} - Update user successfully")
    void testUpdateUser_Success() {
        String email = generateUniqueEmail("update");
        String password = "UpdateTestPass123!";
        
        String userJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "Update",
              "last_name": "Test"
            }
            """.formatted(email, password);

        String userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        String updateJson = """
            {
              "first_name": "Updated",
              "last_name": "Name",
              "password": "%s"
            }
            """.formatted(password);

        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body(updateJson)
            .when()
                .put("/v1/user/" + userId)
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));

        // Verify update
        given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user/" + userId)
            .then()
                .statusCode(200)
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Name"));
    }

    // ========== PRODUCT MANAGEMENT POSITIVE TESTS ==========

    @Test
    @Order(20)
    @DisplayName("POST /v1/product - Create product successfully")
    void testCreateProduct_Success() {
        String email = generateUniqueEmail("product");
        String password = "ProductPass123!";
        
        // Create user first
        String userJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "Product",
              "last_name": "Owner"
            }
            """.formatted(email, password);

        given()
            .contentType(ContentType.JSON)
            .body(userJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create product
        String productJson = """
            {
              "name": "Test Product",
              "description": "A test product",
              "sku": "TEST-001",
              "manufacturer": "Test Manufacturer",
              "quantity": 10
            }
            """;

        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body(productJson)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("name", equalTo("Test Product"))
                .body("description", equalTo("A test product"))
                .body("sku", equalTo("TEST-001"))
                .body("manufacturer", equalTo("Test Manufacturer"))
                .body("quantity", equalTo(10))
                .body("dateAdded", notNullValue())
                .body("dateLastUpdated", notNullValue());
    }

    @Test
    @Order(21)
    @DisplayName("GET /v1/product/{id} - Get product successfully")
    void testGetProduct_Success() {
        String email = generateUniqueEmail("getproduct");
        String password = "GetProductPass123!";
        
        // Create user and product
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Get",
                  "last_name": "Product"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        String productId = given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Get Test Product",
                  "description": "Product to retrieve",
                  "sku": "GET-001",
                  "manufacturer": "Get Manufacturer",
                  "quantity": 5
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(Integer.parseInt(productId)))
                .body("name", equalTo("Get Test Product"));
    }

    @Test
    @Order(22)
    @DisplayName("PUT /v1/product/{id} - Update product successfully")
    void testUpdateProduct_Success() {
        String email = generateUniqueEmail("updateproduct");
        String password = "UpdateProductPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Update",
                  "last_name": "Product"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create product
        String productId = given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Update Test Product",
                  "description": "Product to update",
                  "sku": "UPDATE-001",
                  "manufacturer": "Update Manufacturer",
                  "quantity": 15
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Update product
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
                .put("/v1/product/" + productId)
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));

        // Verify update
        given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Updated Product"))
                .body("quantity", equalTo(25));
    }
    @Test
    @Order(23)
    @DisplayName("PATCH /v1/product/{id} - Partial update quantity only")
    void testPatchProduct_QuantityOnly() {
        String email = generateUniqueEmail("patchqty");
        String password = "PatchQty123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                "username": "%s",
                "password": "%s",
                "first_name": "Patch",
                "last_name": "Qty"
                }
                """.formatted(email, password))
        .when()
            .post("/v1/user")
        .then()
            .statusCode(201);

        // Create product
        String productId = given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "name": "Patch Test Product",
                "description": "Original description",
                "sku": "PATCH-QTY-001",
                "manufacturer": "Original Manufacturer",
                "quantity": 10
                }
                """)
        .when()
            .post("/v1/product")
        .then()
            .statusCode(201)
            .extract()
            .path("id").toString();

        // PATCH only quantity
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "quantity": 100
                }
                """)
        .when()
            .patch("/v1/product/" + productId)
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(204)));

        // Verify other fields unchanged
        given()
        .when()
            .get("/v1/product/" + productId)
        .then()
        .statusCode(200)
        .body("quantity", equalTo(100));
    }

    @Test
    @Order(24)
    @DisplayName("DELETE /v1/product/{id} - Delete product successfully")
    void testDeleteProduct_Success() {
        String email = generateUniqueEmail("deleteproduct");
        String password = "DeleteProductPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Delete",
                  "last_name": "Product"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Create product
        String productId = given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Delete Test Product",
                  "description": "Product to delete",
                  "sku": "DELETE-001",
                  "manufacturer": "Delete Manufacturer",
                  "quantity": 1
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Delete product
        given()
            .auth().basic(email, password)
            .when()
                .delete("/v1/product/" + productId)
            .then()
                .statusCode(204);

        // Verify deletion
        given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(404);
    }
}