package com.marketplace.platform;

import com.marketplace.platform.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import com.marketplace.platform.exception.EnvironmentValidationException;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)  // Add this annotation
public class MarketplacePlatformApplication {

	public static void main(String[] args) {
		try {
			ConfigurableApplicationContext context = SpringApplication.run(MarketplacePlatformApplication.class, args);
		} catch (EnvironmentValidationException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			if (e.getClass().getName().contains("SilentExitException")) {
				return;
			}
			System.err.println("\nFATAL: Application failed to start");
			System.err.println("Cause: " + e.getMessage());
			System.exit(1);
		}
	}
}

//package com.marketplace.platform;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
//
//@SpringBootApplication
//@ConfigurationPropertiesScan("com.marketplace.platform.config")
//public class MarketplacePlatformApplication {
//	public static void main(String[] args) {
//		SpringApplication.run(MarketplacePlatformApplication.class, args);
//	}
//}