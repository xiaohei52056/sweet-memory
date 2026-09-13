package com.sweetmemory.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.* 配置项：上传目录、静态前缀、JWT、管理员账号
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 上传文件存储根目录 */
    private String uploadDir = "./data/uploads";

    /** 照片静态访问前缀 */
    private String publicBase = "/uploads";

    /** 前端构建产物目录（空 = 不托管前端，本地开发用 Vite） */
    private String webDir = "";

    /** 前端托管的基础路径（子路径部署，如 /mem） */
    private String webBase = "/mem";

    private Jwt jwt = new Jwt();

    private List<Admin> admins = new ArrayList<>();

    public static class Jwt {
        private String secret;
        private int expireDays = 7;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpireDays() {
            return expireDays;
        }

        public void setExpireDays(int expireDays) {
            this.expireDays = expireDays;
        }
    }

    public static class Admin {
        private String username;
        private String passwordHash;
        private String nickname;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getPublicBase() {
        return publicBase;
    }

    public void setPublicBase(String publicBase) {
        this.publicBase = publicBase;
    }

    public String getWebDir() {
        return webDir;
    }

    public void setWebDir(String webDir) {
        this.webDir = webDir;
    }

    public String getWebBase() {
        return webBase;
    }

    public void setWebBase(String webBase) {
        this.webBase = webBase;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public List<Admin> getAdmins() {
        return admins;
    }

    public void setAdmins(List<Admin> admins) {
        this.admins = admins;
    }
}
