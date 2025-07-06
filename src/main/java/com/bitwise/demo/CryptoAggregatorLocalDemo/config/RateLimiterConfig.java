package com.bitwise.demo.CryptoAggregatorLocalDemo.config;

import com.bitwise.demo.CryptoAggregatorLocalDemo.filter.RateLimiterFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimiterFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimiterFilter());
        registrationBean.addUrlPatterns("/*"); // Register filter for API endpoints
        return registrationBean;
    }
}