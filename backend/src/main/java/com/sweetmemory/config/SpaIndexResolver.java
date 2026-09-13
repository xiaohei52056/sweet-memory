package com.sweetmemory.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * SPA history 路由回退：{webBase}/** 下找不到实体文件时返回 index.html。
 * 空路径（/mem/）与目录同样回退；越界路径（..）亦回退，防目录穿越。
 */
class SpaIndexResolver extends PathResourceResolver {

    private final Path root;

    SpaIndexResolver(Path root) {
        this.root = root;
    }

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        Path candidate = root.resolve(resourcePath).normalize();
        if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
            return new FileSystemResource(candidate);
        }
        return new FileSystemResource(root.resolve("index.html"));
    }
}
