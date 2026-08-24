package com.example.collab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI collabOpenAPI() {
		return new OpenAPI().info(new Info()
				.title("Project Collab API")
				.description("프로젝트 협업 서비스 API")
				.version("v1"));
	}
}
