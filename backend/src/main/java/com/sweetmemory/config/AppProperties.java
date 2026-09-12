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
