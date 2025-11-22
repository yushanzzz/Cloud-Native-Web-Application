package com.healthcheck.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import com.healthcheck.entity.User;
import com.healthcheck.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final UserService userService;
    
    @Autowired
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.authenticationEntryPoint(basicAuthenticationEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/healthz").permitAll()
                .requestMatchers("/actuator/**").permitAll()  
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/user").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/user/verify").permitAll()  // ← 改成這樣！明確指定GET
                .requestMatchers("/validateEmail").permitAll()  // ← 添加這行
                
                // All GET product endpoints are public
                .requestMatchers(HttpMethod.GET, "/v1/product").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/product/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/product/*/image").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/product/*/image/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/product/**").permitAll()
                
                // Authenticated endpoints
                .requestMatchers(HttpMethod.GET, "/v1/user/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/v1/user/**").authenticated()
                
                // Product write operations require authentication
                .requestMatchers(HttpMethod.POST, "/v1/product/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/v1/product/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/v1/product/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/v1/product/**").authenticated()
                
                // Default: require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(username -> {
            User user = userService.findUserByEmail(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with email: " + username);
            }
            
            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();
        });
        
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("webapp");
        return entryPoint;
    }
}