package com.tron.pay.config;

import com.tron.pay.handler.GlobalInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private GlobalInterceptor globalInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册全局拦截器，并设置拦截路径
        registry
                .addInterceptor(globalInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/logout",
                        "/api/register" ,
                        "/api/createOrder",
                        "/api/queryOrder",
                        "/api/notifyUrl",
                        "/api/loginStatus",
                        "/login","/register"
                        );
    }
}