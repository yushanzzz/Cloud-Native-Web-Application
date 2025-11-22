package com.healthcheck.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    
    @Column(name = "username", unique = true, nullable = false)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Column(nullable = false)
    @JsonIgnore
    @NotBlank(message = "Password is required")
    private String password;
    
    @JsonProperty("first_name")
    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @JsonProperty("last_name")
    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @Column(name = "account_created", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime accountCreated;
    
    @Column(name = "account_updated", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime accountUpdated;
    
    // Email verification fields
    @Column(name = "is_verified", nullable = false)
    @JsonIgnore
    private boolean isVerified = false;
    
    @Column(name = "verification_token")
    @JsonIgnore
    private String verificationToken;
    
    @Column(name = "verification_expiry")
    @JsonIgnore
    private LocalDateTime verificationExpiry;
    
    public User() {}
    
    public User(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isVerified = false; // New users are not verified by default
    }
    
    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }
   
    // 修改 getter 方法名稱和註解
    @JsonProperty("username")
    public String getUsername() {  // 改名稱
        return email;
    }

    @JsonProperty("username")
    public void setUsername(String email) {  // 改名稱
        this.email = email;
    }
    
    public String getPassword() { 
        return password; 
    }
    public void setPassword(String password) { 
        this.password = password; 
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
    
    // Email verification getters/setters
    public boolean isVerified() {
        return isVerified;
    }
    
    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }
    
    public String getVerificationToken() {
        return verificationToken;
    }
    
    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
    
    public LocalDateTime getVerificationExpiry() {
        return verificationExpiry;
    }
    
    public void setVerificationExpiry(LocalDateTime verificationExpiry) {
        this.verificationExpiry = verificationExpiry;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.accountCreated = now;
        this.accountUpdated = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.accountUpdated = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + email + '\'' +
                ", accountCreated=" + accountCreated +
                ", accountUpdated=" + accountUpdated +
                '}';
    }
}