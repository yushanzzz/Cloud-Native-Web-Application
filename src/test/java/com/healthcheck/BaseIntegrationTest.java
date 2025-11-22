package com.healthcheck;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


/**
 * Base class for all integration tests
 * Sets up REST Assured configuration and test environment
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")

public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Set default content type for all requests
        RestAssured.given().contentType(ContentType.JSON);
    }

    /**
     * Helper method to create Basic Auth header
     */
    protected String createBasicAuth(String username, String password) {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }
    
    /**
     * Helper method to generate test user data
     */
    protected String createUserJson(String email, String password, String firstName, String lastName) {
        return String.format("""
            {
              "username": "%s",
              "password": "%s",
              "first_name": "%s",
              "last_name": "%s"
            }
            """, email, password, firstName, lastName);
    }
    
    /**
     * Helper method to generate test product data
     */
    protected String createProductJson(String name, String description, String sku, 
                                     String manufacturer, int quantity) {
        return String.format("""
            {
              "name": "%s",
              "description": "%s",
              "sku": "%s",
              "manufacturer": "%s",
              "quantity": %d
            }
            """, name, description, sku, manufacturer, quantity);
    }
  
    protected void validateResponseTime(long startTime, long maxDurationMs) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > maxDurationMs) {
            throw new AssertionError("Response time " + duration + "ms exceeded maximum " + maxDurationMs + "ms");
        }
    }

     protected void executeConcurrentRequests(Runnable request, int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(request);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
    
}