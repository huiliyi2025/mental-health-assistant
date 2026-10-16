package org.example.aispringboot.config;

import jakarta.servlet.http.HttpServletRequest;
import org.example.aispringboot.service.UserService;
import org.example.aispringboot.util.JwtAuthticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;

/**
 * Spring Security 配置
 * 采用 JWT 无状态认证：自定义的 JwtAuthticationFilter 在每次请求时解析 token，
 * 因此关闭 CSRF 与 Session，并把认证过滤器放在 UsernamePasswordAuthenticationFilter 之前。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    // 无需登录即可访问的路径（不限方法）
    private static final String[] PUBLIC_PATHS = {
            "/",
            "/api/test",
            "/api/user/login",
            "/api/user/add"
    };

    // 仅 GET 请求免登录的路径（知识文章阅读、静态文件、接口文档）
    private static final String[] PUBLIC_GET_PATHS = {
            "/api/knowledge/category/tree",
            "/api/knowledge/article/page",
            "/api/knowledge/article/*",
            "/files/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    /**
     * 判断指定 URI 是否为公开路径（不限方法）
     */
    public static boolean isPublicPath(String requestUri) {
        for (String publicPath : PUBLIC_PATHS) {
            if (antPathMatcher.match(publicPath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断请求是否无需认证（结合请求方法）。
     */
    public static boolean isPublicPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (isPublicPath(uri)) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod())) {
            for (String publicPath : PUBLIC_GET_PATHS) {
                if (antPathMatcher.match(publicPath, uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserService userService) throws Exception {
        http
                // 启用 CORS（使用 CorsConfig 中定义的规则）
                .cors(org.springframework.security.config.Customizer.withDefaults())
                // 禁用 CSRF 保护（API 服务通常不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置会话管理为无状态（JWT 需要）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求的授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开的路径，无需登录即可访问
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // 公开的 GET 请求（文章阅读、静态文件）
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                        // 其他请求都需要认证
                        .anyRequest().authenticated()
                )
                // 添加 JWT 认证过滤器（直接实例化，避免被 Servlet 容器重复注册）
                .addFilterBefore(new JwtAuthticationFilter(userService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
