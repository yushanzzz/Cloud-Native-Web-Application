package com.healthcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SnsService {
    
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String snsTopicArn;
    
    public SnsService(
            @Value("${aws.sns.topic.arn:#{null}}") String snsTopicArn) {
        this.snsClient = SnsClient.builder().build();
        this.objectMapper = new ObjectMapper();
        this.snsTopicArn = snsTopicArn;
    }
    
    /**
     * Publish user verification message to SNS topic
     * @param email User's email address
     * @param verificationToken UUID verification token
     * @param firstName User's first name
     */
    public void publishUserVerificationMessage(String email, String verificationToken, String firstName) {
        if (snsTopicArn == null || snsTopicArn.isEmpty()) {
            log.warn("SNS Topic ARN not configured, skipping message publication");
            return;
        }
        
        try {
            // Create message payload
            Map<String, String> messageData = new HashMap<>();
            messageData.put("email", email);
            messageData.put("token", verificationToken);
            messageData.put("firstName", firstName != null ? firstName : "User");
            
            // Convert to JSON
            String messageJson = objectMapper.writeValueAsString(messageData);
            
            // Create SNS publish request
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .message(messageJson)
                    .subject("User Email Verification")
                    .build();
            
            // Publish message
            PublishResponse response = snsClient.publish(publishRequest);
            
            log.info("Successfully published user verification message to SNS. MessageId: {}, Email: {}", 
                       response.messageId(), email);
                       
        } catch (SnsException e) {
            log.error("Failed to publish message to SNS topic: {} for email: {}", 
                        e.getMessage(), email, e);
            throw new RuntimeException("Failed to send verification email", e);
            
        } catch (Exception e) {
            log.error("Unexpected error while publishing to SNS for email: {}", email, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    /**
     * Check if SNS is properly configured
     * @return true if SNS topic ARN is configured
     */
    public boolean isSnsConfigured() {
        return snsTopicArn != null && !snsTopicArn.isEmpty();
    }
}