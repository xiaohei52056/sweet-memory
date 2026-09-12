package com.sweetmemory.tool;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成管理员密码的 bcrypt 哈希，用于填入 application.yml。
 * 运行：mvn -q exec:java -Dexec.mainClass=com.sweetmemory.tool.HashGen -Dexec.args="star2021"
 * 或在 IDE 中直接运行 main。
 */
public final class HashGen {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: HashGen <明文密码>");
            return;
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(args[0]);
        System.out.println("明文: " + args[0]);
        System.out.println("哈希: " + hash);
        System.out.println("yml 中写法: password-hash: \"" + hash + "\"");
    }

    private HashGen() {
    }
}
