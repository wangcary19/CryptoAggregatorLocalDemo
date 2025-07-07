package com.bitwise.demo.CryptoAggregatorLocalDemo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.RATE_LIMITER_LIMIT;

@Component
public class RateLimiterFilter implements Filter {

    // Map to store request counts per IP address
    private final Map<String, AtomicInteger> requestCountsPerIpAddress = new ConcurrentHashMap<>();

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
        if (requests > RATE_LIMITER_LIMIT) {
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
