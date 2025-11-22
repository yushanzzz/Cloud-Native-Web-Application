package com.healthcheck.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public class UserUpdateRequest {
    
    @JsonProperty("first_name")
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @JsonProperty("last_name")
    @NotBlank(message = "Last name is required")
    private String lastName;

    private String password;

    @JsonProperty("username")
    private String username;

    public UserUpdateRequest() {}

    public UserUpdateRequest(String firstName, String lastName, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}