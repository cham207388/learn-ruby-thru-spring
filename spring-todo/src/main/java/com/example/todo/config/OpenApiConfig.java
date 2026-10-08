package com.example.todo.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI todoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Todo API (Spring)")
                        .version("v1")
                        .description("Same contract as the Rails app: snake_case JSON, RFC 9457 Problem Details"));
    }

    // swagger-core builds schemas with its own Jackson 2 mapper, so it does not see
    // spring.jackson.property-naming-strategy. Match it here or the docs show camelCase.
    @Bean
    ModelResolver snakeCaseModelResolver() {
        return new ModelResolver(Json.mapper().copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
    }
}
