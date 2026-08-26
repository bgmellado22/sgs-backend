package com.conectatech.sgs_backend.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.lang.NonNull;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Override
    @NonNull
    protected String getDatabaseName() {
        return "sgs_db";
    }

    @Override
    @NonNull
    public MongoClient mongoClient() {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null || uri.isEmpty()) {
            uri = System.getenv("SPRING_DATA_MONGODB_URI");
        }
        
        if (uri == null || uri.isEmpty()) {
            logger.warn("No MONGODB_URI found in environment variables! Falling back to localhost.");
            uri = "mongodb://localhost:27017/sgs_db";
        } else {
            // Mask password in logs
            String maskedUri = uri.replaceAll(":[^:]+@", ":***@");
            logger.info("Initializing MongoDB connection with URI: {}", maskedUri);
        }

        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build();
        
        return MongoClients.create(mongoClientSettings);
    }
}
