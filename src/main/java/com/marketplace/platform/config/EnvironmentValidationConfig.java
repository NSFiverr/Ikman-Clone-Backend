package com.marketplace.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentValidationConfig {

    @Bean
    public static PropertyValidatorBeanFactoryPostProcessor propertyValidatorBeanFactoryPostProcessor() {
        return new PropertyValidatorBeanFactoryPostProcessor();
    }
}