package com.sweetmemory.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.sweetmemory.config.AppProperties;

/**
 * 管理员登录：账号写死在配置，bcrypt 校验
 */
@Service
public class AuthService {

    private final AppProperties props;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(AppProperties props, JwtService jwtService) {
        this.props = props;
        this.jwtService = jwtService;
    }

    /** 登录成功返回 {username, nickname, token}，失败返回 null */
    public Map<String, String> login(String username, String password) {
        List<AppProperties.Admin> admins = props.getAdmins();
        for (AppProperties.Admin a : admins) {
            if (a.getUsername().equals(username)
                    && encoder.matches(password, a.getPasswordHash())) {
                return Map.of(
                        "username", a.getUsername(),
                        "nickname", a.getNickname(),
                        "token", jwtService.issue(a.getUsername()));
            }
        }
        return null;
    }
}
