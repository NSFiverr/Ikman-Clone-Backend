package com.marketplace.platform.config;

import com.marketplace.platform.exception.EnvironmentValidationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyValidatorBeanFactoryPostProcessor implements BeanFactoryPostProcessor, PriorityOrdered {

    private static final List<String> REQUIRED_ENV_VARS = Arrays.asList(
            "DB_USERNAME",
            "DB_PASSWORD",
            "MAIL_USERNAME",
            "MAIL_PASSWORD",
            "JWT_SECRET",
            "APP_EMAIL_FROM"
    );

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Environment environment = beanFactory.getBean(Environment.class);
        List<String> missingVars = REQUIRED_ENV_VARS.stream()
                .filter(var -> {
                    String value = environment.getProperty(var);
                    return value == null || value.trim().isEmpty();
                })
                .collect(Collectors.toList());

        if (!missingVars.isEmpty()) {
            throw new EnvironmentValidationException(generateErrorMessage(missingVars));
        }
    }

    private String generateErrorMessage(List<String> missingVars) {
        return "\nMissing Required Environment Variables:\n" +
                "----------------------------------------\n" +
                String.join("\n", missingVars.stream()
                        .map(var -> "- " + var)
                        .collect(Collectors.toList())) +
                "\n----------------------------------------";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}