package com.healthcheck.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthcheck.entity.User;

public class UserResponse {
    private Long id;

    @JsonProperty("username")
    private String email;
    
    private String firstName;
    private String lastName;
    private LocalDateTime accountCreated;
    private LocalDateTime accountUpdated;

    public UserResponse(){}

    public UserResponse(User user){
        this.id = user.getId();
        this.email = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.accountCreated = user.getAccountCreated();
        this.accountUpdated = user.getAccountUpdated();
    }

    public UserResponse(Long id, String email, String firstName, String lastName, 
                        LocalDateTime accountCreated, LocalDateTime accountUpdated){
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountCreated = accountCreated;
        this.accountUpdated = accountUpdated;
    }                       
    
    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getEmail() { 
        return email; 
    }
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public String getFirstName() { 
        return firstName; 
    }
    public void setFirstName(String firstName) { 
        this.firstName = firstName; 
    }
    
    public String getLastName() { 
        return lastName; 
    }
    public void setLastName(String lastName) { 
        this.lastName = lastName; 
    }
    
    public LocalDateTime getAccountCreated() { 
        return accountCreated; 
    }
    public void setAccountCreated(LocalDateTime accountCreated) {
        this.accountCreated = accountCreated; 
    }
    
    public LocalDateTime getAccountUpdated() { 
        return accountUpdated; 
    }
    public void setAccountUpdated(LocalDateTime accountUpdated) { 
        this.accountUpdated = accountUpdated; 
    }

    @Override
    public String toString(){
        return "UserResponse{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", accountCreated=" + accountCreated +
                ", accountUpdated=" + accountUpdated +
                '}';
    }
}
