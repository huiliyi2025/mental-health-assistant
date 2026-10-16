package org.example.aispringboot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 接口文档配置。
 * 启动后访问：http://localhost:1236/swagger-ui.html
 * 文档中通过右上角 Authorize 填入登录返回的 token 即可调试需要鉴权的接口。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "token";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("心理健康助手 API")
                        .description("基于 Spring AI + Vue3 的心理健康助手后端接口文档")
                        .version("v1.0"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("token")
                                .description("登录后返回的 JWT token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
