package org.example.aispringboot.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.config.SecurityConfig;
import org.example.aispringboot.enumClass.UserStatus;
import org.example.aispringboot.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器
 * 在请求进入 Controller 之前执行，负责：
 * <ol>
 *     <li>从请求头提取 JWT token；</li>
 *     <li>校验 token 的合法性与有效期；</li>
 *     <li>校验用户状态（是否被禁用）；</li>
 *     <li>将认证信息写入 SecurityContextHolder。</li>
 * </ol>
 */
@Slf4j
public class JwtAuthticationFilter extends OncePerRequestFilter {

    private final UserService userService;

    public JwtAuthticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 公开路径无需认证，直接跳过本过滤器
        return SecurityConfig.isPublicPath(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // 提取 JWT token
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            // 未携带 token，直接拒绝并终止过滤链
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }

        // 验证 token 并获取用户信息
        JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);
        if (validationResult == null || !validationResult.isValid()) {
            ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            return;
        }

        // 查询用户并校验状态
        var user = userService.getUserById(validationResult.getUserId());
        if (user == null || !UserStatus.NORMAL.getCode().equals(user.getStatus())) {
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }

        // 创建 Spring Security 认证对象（角色映射：2-管理员，1-普通用户）
        String roleName = (validationResult.getRoleType() != null
                && validationResult.getRoleType().equals(org.example.aispringboot.enumClass.UserType.ADMIN.getCode()))
                ? "ADMIN" : "USER";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleName)
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                validationResult.getUsername(), // 用户名作为主体
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 将 token 存储到请求属性中，供 Controller 读取当前登录用户
        request.setAttribute("jwtToken", token);

        // 继续过滤器链
        chain.doFilter(request, response);
    }
}
