package com.sweetmemory.web;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sweetmemory.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 鉴权拦截器：/api/** 需携带 Authorization: Bearer <token>（/api/login 除外）
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // CORS 预检放行
        }
        // 访客可读：照片列表（回收站除外）
        if ("GET".equalsIgnoreCase(request.getMethod())
                && "/api/photos".equals(request.getRequestURI())
                && !"true".equals(request.getParameter("trashed"))) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String username = jwtService.verify(header.substring(7));
            if (username != null) {
                request.setAttribute("authUser", username);
                return true;
            }
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"未登录或登录已过期\"}");
        return false;
    }
}
