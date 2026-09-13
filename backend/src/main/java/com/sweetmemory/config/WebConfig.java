package com.sweetmemory.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.sweetmemory.web.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;
    private final AuthInterceptor authInterceptor;

    public WebConfig(AppProperties props, AuthInterceptor authInterceptor) {
        this.props = props;
        this.authInterceptor = authInterceptor;
    }

    /** 上传目录静态映射：{publicBase}/** → data/uploads/ */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Paths.get(props.getUploadDir()).toAbsolutePath().normalize().toString();
        registry.addResourceHandler(props.getPublicBase() + "/**")
                .addResourceLocations("file:" + dir + "/")
                .setCachePeriod(30 * 24 * 3600);

        // 一站式部署：由本服务直接托管前端构建产物（生产无 Nginx 时使用）
        String webDir = props.getWebDir();
        if (webDir != null && !webDir.isBlank()) {
            String base = props.getWebBase(); // 如 /mem
            String loc = "file:" + Paths.get(webDir).toAbsolutePath().normalize() + "/";
            registry.addResourceHandler(base + "/**")
                    .addResourceLocations(loc)
                    .resourceChain(true)
                    .addResolver(new SpaIndexResolver(Paths.get(webDir).toAbsolutePath().normalize()));
        }
    }

    /**
     * /mem 与 /mem/ 是目录式 URL，Spring 的 welcome-page 处理会先于静态资源解析并返回 404，
     * 这里显式转发到 /mem/index.html（其余 /mem/xxx 由 SpaIndexResolver 兜底）。
     */
    @Override
    public void addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
        String base = props.getWebBase();
        if (props.getWebDir() != null && !props.getWebDir().isBlank()) {
            registry.addViewController(base).setViewName("forward:" + base + "/index.html");
            registry.addViewController(base + "/").setViewName("forward:" + base + "/index.html");
        }
    }

    /** 开发环境跨域：Vite dev server 直连 8082 时免配代理 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login");
    }
}
