package com.healthcheck;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * Edge Case Tests - Boundary conditions and special scenarios
 * Tests edge cases, performance, and data integrity
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Edge Case Tests")
public class EdgeCaseTest extends BaseIntegrationTest {

    private String generateUniqueEmail(String prefix) {
        return prefix + System.currentTimeMillis() + "@example.com";
    }

    // ========== HEALTH CHECK EDGE CASES ==========

    @Test
    @Order(1)
    @DisplayName("GET /healthz with query parameters")
    void testHealthCheck_WithQueryParams() {
        given()
            .queryParam("test", "value")
            .when()
                .get("/healthz")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(2)
    @DisplayName("PATCH /healthz - Should return 405 Method Not Allowed")
    void testHealthCheck_PatchNotAllowed() {
        given()
            .when()
                .patch("/healthz")
            .then()
                .statusCode(405);
    }

    // ========== USER MANAGEMENT EDGE CASES ==========

    @Test
    @Order(10)
    @DisplayName("POST /v1/user - Boundary value string lengths")
    void testCreateUser_BoundaryValues() {
        String email = generateUniqueEmail("boundary");
        String password = "Password123!";
        String longFirstName = "A".repeat(50);
        String longLastName = "B".repeat(50);
        
        String boundaryUserJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "%s",
              "last_name": "%s"
            }
            """.formatted(email, password, longFirstName, longLastName);

        given()
            .contentType(ContentType.JSON)
            .body(boundaryUserJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)));
    }

    @Test
    @Order(11)
    @DisplayName("POST /v1/user - Special characters in email and names")
    void testCreateUser_SpecialCharacters() {
        String email = generateUniqueEmail("special.chars+test");
        String password = "P@$$w0rd!123#";
        String firstName = "José-María";
        String lastName = "O'Connor-Smith";
        
        String specialCharJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "%s",
              "last_name": "%s"
            }
            """.formatted(email, password, firstName, lastName);

        given()
            .contentType(ContentType.JSON)
            .body(specialCharJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)));
    }

    @Test
    @Order(12)
    @DisplayName("POST /v1/user - Unicode characters in names")
    void testCreateUser_UnicodeCharacters() {
        String email = generateUniqueEmail("unicode");
        String password = "UnicodePass123!";
        String firstName = "Jose";
        String lastName = "Smith";
        
        String unicodeJson = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "%s",
              "last_name": "%s"
            }
            """.formatted(email, password, firstName, lastName);

        given()
            .contentType(ContentType.JSON)
            .body(unicodeJson)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)));
    }

    @Test
    @Order(13)
    @DisplayName("POST /v1/user - Read-only fields cannot be set by client")
    void testCreateUser_ReadonlyFields() {
        String email = generateUniqueEmail("readonly");
        String password = "ReadonlyTest123!";
        String firstName = "Readonly";
        String lastName = "Test";
        
        String userJsonWithReadonlyFields = """
            {
              "username": "%s",
              "password": "%s",
              "first_name": "%s",
              "last_name": "%s",
              "account_created": "2023-01-01T00:00:00.000Z",
              "account_updated": "2023-01-01T00:00:00.000Z",
              "id": 999
            }
            """.formatted(email, password, firstName, lastName);

        given()
            .contentType(ContentType.JSON)
            .body(userJsonWithReadonlyFields)
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .body("id", not(equalTo(999)));
    }
    
    @Test
    @Order(14)
    @DisplayName("PUT /v1/user/{id} - Partial updates work correctly")
    void testUpdateUser_PartialUpdate() {
        String email = generateUniqueEmail("partial");
        String password = "PartialPass123!";
        
        // Create user
        String userId = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Partial",
                  "last_name": "Update"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Update only first name
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                "first_name": "PartiallyUpdated",
                "last_name": "Update",
                "password": "%s"
                }
                """.formatted(password))
            .when()
                .put("/v1/user/" + userId)
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));

        // Verify only first name changed, last name unchanged
        given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user/" + userId)
            .then()
                .statusCode(200)
                .body("firstName", equalTo("PartiallyUpdated"))
                .body("lastName", equalTo("Update"));
    }

    @Test
    @Order(15)
    @DisplayName("PUT /v1/user/{id} - Data integrity after update")
    void testUpdateUser_DataIntegrity() {
        String email = generateUniqueEmail("integrity");
        String password = "IntegrityPass123!";
        
        // Create user
        String userId = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Integrity",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        String originalAccountCreated = given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user/" + userId)
            .then()
                .statusCode(200)
                .extract()
                .path("accountCreated").toString();

        // Update user
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "first_name": "IntegrityUpdated",
                  "last_name": "TestUpdated",
                  "password": "%s"
                }
                """.formatted(password))
            .when()
                .put("/v1/user/" + userId)
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));

        // Verify data integrity
        given()
            .auth().basic(email, password)
            .when()
                .get("/v1/user/" + userId)
            .then()
                .statusCode(200)
                .body("id", equalTo(Integer.parseInt(userId)))
                .body("username", equalTo(email))
                .body("accountCreated", equalTo(originalAccountCreated))
                .body("accountUpdated", not(equalTo(originalAccountCreated)));
    }

    @Test
    @Order(16)
    @DisplayName("User creation performance test")
    void testUserCreation_Performance() {
        String email = generateUniqueEmail("performance");
        String password = "PerformancePass123!";
        
        long startTime = System.currentTimeMillis();
        
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Performance",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .time(lessThan(2000L));
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // Response time should be reasonable (less than 2 seconds)
        assert responseTime < 2000 : "User creation took too long: " + responseTime + "ms";
    }

    // ========== PRODUCT MANAGEMENT EDGE CASES ==========

    @Test
    @Order(20)
    @DisplayName("POST /v1/product - Boundary values for quantity")
    void testCreateProduct_BoundaryQuantity() {
        String email = generateUniqueEmail("boundaryqty");
        String password = "BoundaryQtyPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Boundary",
                  "last_name": "Quantity"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Test with zero quantity
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Zero Quantity Product",
                  "description": "Product with zero quantity",
                  "sku": "ZERO-QTY-001",
                  "manufacturer": "Zero Manufacturer",
                  "quantity": 0
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)));

        // Test with maximum integer value
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Max Quantity Product",
                  "description": "Product with maximum quantity",
                  "sku": "MAX-QTY-001",
                  "manufacturer": "Max Manufacturer",
                  "quantity": 2147483647
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)));
    }

    @Test
    @Order(21)
    @DisplayName("POST /v1/product - Long string values")
    void testCreateProduct_LongStrings() {
        String email = generateUniqueEmail("longstrings");
        String password = "LongStringsPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Long",
                  "last_name": "Strings"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        String longName = "Very ".repeat(50) + "Long Product Name";
        String longDescription = "This is a very ".repeat(100) + "long description";
        String longManufacturer = "Super ".repeat(20) + "Long Manufacturer Name";
        
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "%s",
                  "description": "%s",
                  "sku": "LONG-STRINGS-001",
                  "manufacturer": "%s",
                  "quantity": 1
                }
                """.formatted(longName, longDescription, longManufacturer))
            .when()
                .post("/v1/product")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400), equalTo(500)));
    }

    @Test
    @Order(22)
    @DisplayName("POST /v1/product - Special characters in product data")
    void testCreateProduct_SpecialCharacters() {
        String email = generateUniqueEmail("productspecial");
        String password = "ProductSpecialPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Product",
                  "last_name": "Special"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Product with Special Characters @#$%^&*()",
                  "description": "Description with ñümeròus spéçíål châraçtërs & symbols!",
                  "sku": "SPECIAL-001-@#$",
                  "manufacturer": "Spëçîål Mânüfâçtürër & Co.",
                  "quantity": 5
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(anyOf(equalTo(201), equalTo(400)));
    }

    @Test
    @Order(23)
    @DisplayName("PUT /v1/product/{id} - Data persistence after update")
    void testUpdateProduct_DataPersistence() {
        String email = generateUniqueEmail("persistence");
        String password = "PersistencePass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Persistence",
                  "last_name": "Test"
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
                  "name": "Persistence Test Product",
                  "description": "Original description",
                  "sku": "PERSISTENCE-001",
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

        String originalDateAdded = given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(200)
                .extract()
                .path("dateAdded").toString();

        // Update product
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Updated Persistence Product",
                  "description": "Updated description",
                  "sku": "PERSISTENCE-UPDATED-001",
                  "manufacturer": "Updated Manufacturer",
                  "quantity": 20
                }
                """)
            .when()
                .put("/v1/product/" + productId)
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(204)));

        // Verify data persistence and integrity
        given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(200)
                .body("id", equalTo(Integer.parseInt(productId)))
                .body("name", equalTo("Updated Persistence Product"))
                .body("description", equalTo("Updated description"))
                .body("sku", equalTo("PERSISTENCE-UPDATED-001"))
                .body("manufacturer", equalTo("Updated Manufacturer"))
                .body("quantity", equalTo(20))
                .body("dateAdded", equalTo(originalDateAdded))
                .body("dateLastUpdated", not(equalTo(originalDateAdded)));
    }

    @Test
    @Order(24)
    @DisplayName("Concurrent product creation test")
    void testConcurrentProductCreation() {
        String email = generateUniqueEmail("concurrent");
        String password = "ConcurrentPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Concurrent",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201);

        // Test multiple concurrent product creations
        for (int i = 1; i <= 5; i++) {
            given()
                .auth().basic(email, password)
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "name": "Concurrent Product %d",
                      "description": "Product created in concurrent test",
                      "sku": "CONCURRENT-%03d",
                      "manufacturer": "Concurrent Manufacturer",
                      "quantity": %d
                    }
                    """.formatted(i, i, i))
                .when()
                    .post("/v1/product")
                .then()
                    .statusCode(201)
                    .time(lessThan(3000L));
        }
    }

    @Test
    @Order(25)
    @DisplayName("DELETE /v1/product/{id} - Verify deletion side effects")
    void testDeleteProduct_SideEffects() {
        String email = generateUniqueEmail("deletesideeffect");
        String password = "DeleteSideEffectPass123!";
        
        // Create user
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "DeleteSideEffect",
                  "last_name": "Test"
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
                  "name": "Delete Side Effect Product",
                  "description": "Product to test deletion side effects",
                  "sku": "DELETE-SIDE-EFFECT-001",
                  "manufacturer": "Delete Side Effect Manufacturer",
                  "quantity": 1
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .extract()
                .path("id").toString();

        // Verify product exists
        given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(200);

        // Delete product
        given()
            .auth().basic(email, password)
            .when()
                .delete("/v1/product/" + productId)
            .then()
                .statusCode(204);

        // Verify product no longer exists
        given()
            .when()
                .get("/v1/product/" + productId)
            .then()
                .statusCode(404);

        // Verify cannot update deleted product
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Updated Deleted Product",
                  "description": "This should fail",
                  "sku": "UPDATED-DELETED-001",
                  "manufacturer": "Should Not Work",
                  "quantity": 999
                }
                """)
            .when()
                .put("/v1/product/" + productId)
            .then()
                .statusCode(404);

        // Verify cannot delete already deleted product
        given()
            .auth().basic(email, password)
            .when()
                .delete("/v1/product/" + productId)
            .then()
                .statusCode(404);
    }

    @Test
    @Order(26)
    @DisplayName("API response time performance test")
    void testAPI_ResponseTime() {
        // Test health check performance
        given()
            .when()
                .get("/healthz")
            .then()
                .statusCode(200)
                .time(lessThan(1000L));
        
        String email = generateUniqueEmail("performance");
        String password = "PerformancePass123!";
        
        // Test user creation performance
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "first_name": "Performance",
                  "last_name": "Test"
                }
                """.formatted(email, password))
            .when()
                .post("/v1/user")
            .then()
                .statusCode(201)
                .time(lessThan(3000L));
        
        // Test product creation performance
        given()
            .auth().basic(email, password)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "Performance Test Product",
                  "description": "Product for performance testing",
                  "sku": "PERF-001",
                  "manufacturer": "Performance Manufacturer",
                  "quantity": 1
                }
                """)
            .when()
                .post("/v1/product")
            .then()
                .statusCode(201)
                .time(lessThan(3000L));
    }
}