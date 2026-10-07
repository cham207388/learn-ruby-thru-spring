package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Kept off the main class so web test slices (@WebMvcTest) do not need JPA beans
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {}
