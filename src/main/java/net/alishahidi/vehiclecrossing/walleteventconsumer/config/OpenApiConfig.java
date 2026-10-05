package net.alishahidi.vehiclecrossing.walleteventconsumer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI consumerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Wallet Event Consumer API")
                .version("v1")
                .description("""
                        Read-only view of what the consumer has done: the wallet events it processed (one row
                        per event, with its commit-to-consumer lag) and totals.
                        Internal service API, no authentication."""));
    }
}
