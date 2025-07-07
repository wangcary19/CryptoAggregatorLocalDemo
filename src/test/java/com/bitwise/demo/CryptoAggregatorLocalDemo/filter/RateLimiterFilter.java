package com.bitwise.demo.CryptoAggregatorLocalDemo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.RATE_LIMITER_LIMIT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RateLimiterFilterTest {

    private RateLimiterFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        filter = new RateLimiterFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void allowsRequests_UnderLimit() throws IOException, ServletException {
        for (int i = 0; i < RATE_LIMITER_LIMIT; i++) {
            filter.doFilter(request, response, chain);
        }
        verify(chain, times(RATE_LIMITER_LIMIT)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void blocksRequests_OverLimit() throws IOException, ServletException {
        for (int i = 0; i < RATE_LIMITER_LIMIT; i++) {
            filter.doFilter(request, response, chain);
        }
        // The next request should be blocked
        filter.doFilter(request, response, chain);

        verify(chain, times(RATE_LIMITER_LIMIT)).doFilter(request, response);
        verify(response).setStatus(429);
        String body = responseWriter.toString();
        assertTrue(body.contains("Exceeded the allowed number of requests"));
    }

}