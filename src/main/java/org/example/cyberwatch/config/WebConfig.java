package org.example.cyberwatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Mappa rot-URL till index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        
        // Valfritt: Om man vill kunna skriva /login istället för /pages/login.html
        registry.addViewController("/login").setViewName("forward:/pages/login.html");
        registry.addViewController("/dashboard").setViewName("forward:/pages/dashboard.html");
    }
}
