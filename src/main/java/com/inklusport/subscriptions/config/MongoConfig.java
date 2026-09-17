package com.inklusport.subscriptions.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Habilita el relleno de {@code @CreatedDate} / {@code @LastModifiedDate}, que sustituye
 * a los {@code @CreationTimestamp} / {@code @UpdateTimestamp} de Hibernate.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
