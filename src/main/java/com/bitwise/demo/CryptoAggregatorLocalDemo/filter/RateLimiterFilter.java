package com.bitwise.demo.CryptoAggregatorLocalDemo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

//@Component
public class RateLimiterFilter implements Filter {

    // Map to store request counts per IP address
    private final Map<String, AtomicInteger> requestCountsPerIpAddress = new ConcurrentHashMap<>();

    // Maximum requests allowed per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // Allow one request per second

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String clientIpAddress = httpServletRequest.getRemoteAddr();

        // Initialize request count for the client IP address
        requestCountsPerIpAddress.putIfAbsent(clientIpAddress, new AtomicInteger(0));
        AtomicInteger requestCount = requestCountsPerIpAddress.get(clientIpAddress);

        // Increment the request count
        int requests = requestCount.incrementAndGet();

        // Check if the request limit has been exceeded
        if (requests > MAX_REQUESTS_PER_MINUTE) {
            httpServletResponse.setStatus(429); // Too Many Requests
            httpServletResponse.getWriter().write("Exceeded the allowed number of requests per minute.  Please wait and try again later.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Scheduled(fixedRate = 60000)
    public void resetRequestCounts() { // This method resets the request record every minute
        requestCountsPerIpAddress.clear();
    }

}
