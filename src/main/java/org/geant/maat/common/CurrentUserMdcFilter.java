package org.geant.maat.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CurrentUserMdcFilter extends OncePerRequestFilter {
    private final CurrentUserResolver currentUserResolver;

    public CurrentUserMdcFilter(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put(CurrentUserResolver.MDC_KEY, currentUserResolver.resolveCurrentUser());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CurrentUserResolver.MDC_KEY);
        }
    }
}
