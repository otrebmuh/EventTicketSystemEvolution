package com.eventbooking.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsMessagingConfig {
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    @Value("${aws.access-key-id:}")
    private String accessKeyId;
    
    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;
    
    @Value("${aws.sqs.endpoint:}")
    private String sqsEndpoint;
    
    @Value("${aws.sns.endpoint:}")
    private String snsEndpoint;
    
    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider());
        
        // For local development with LocalStack
        if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(sqsEndpoint));
        }
        
        return builder.build();
    }
    
    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(awsCredentialsProvider());
        
        // For local development with LocalStack
        if (snsEndpoint != null && !snsEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(snsEndpoint));
        }
        
        return builder.build();
    }
    
    private AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKeyId != null && !accessKeyId.isEmpty() 
                && secretAccessKey != null && !secretAccessKey.isEmpty()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        return DefaultCredentialsProvider.create();
    }
}
