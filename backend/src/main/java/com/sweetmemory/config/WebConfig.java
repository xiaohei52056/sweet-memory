package com.sweetmemory.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sweetmemory.web.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;
    private final AuthInterceptor authInterceptor;

    public WebConfig(AppProperties props, AuthInterceptor authInterceptor) {
        this.props = props;
        this.authInterceptor = authInterceptor;
    }

    /** 上传目录静态映射：/uploads/** → data/uploads/ */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Paths.get(props.getUploadDir()).toAbsolutePath().normalize().toString();
        registry.addResourceHandler(props.getPublicBase() + "/**")
                .addResourceLocations("file:" + dir + "/")
                .setCachePeriod(30 * 24 * 3600); // 生产环境 Nginx 会接管此路径
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
