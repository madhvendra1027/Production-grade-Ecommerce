package com.example.project.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${frontend.url:http://localhost:5173}")
    private String frontEndUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> patterns = new ArrayList<>(List.of(
                "https://*.vercel.app",
                "https://*.onrender.com",
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));

        if (frontEndUrl != null && !frontEndUrl.trim().isEmpty()) {
            Arrays.stream(frontEndUrl.split(","))
                    .map(String::trim)
                    .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                    .filter(url -> !url.isEmpty() && !patterns.contains(url))
                    .forEach(patterns::add);
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(patterns.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Set-Cookie", "Authorization")
                .allowCredentials(true);
    }
}
