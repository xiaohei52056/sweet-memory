package com.sweetmemory.tool;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成管理员密码的 bcrypt 哈希，用于填入 application.yml / env 文件。
 * 用法一：HashGen <明文密码>
 * 用法二：HashGen @<密码文件路径>（密码经文件传入，不落 shell 历史）
 * 在 IDE 中直接运行 main 亦可。
 */
public final class HashGen {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: HashGen <明文密码> 或 HashGen @<密码文件>");
            return;
        }
        String password = args[0];
        if (password.startsWith("@")) {
            try {
                password = java.nio.file.Files.readString(java.nio.file.Paths.get(password.substring(1))).trim();
            } catch (java.io.IOException e) {
                System.out.println("读取密码文件失败: " + e.getMessage());
                return;
            }
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        System.out.println("哈希: " + hash);
        System.out.println("yml 中写法: password-hash: \"" + hash + "\"");
    }

    private HashGen() {
    }
}
