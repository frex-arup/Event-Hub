package com.eventhub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * RBAC authorization filter that enforces role-based access control
 * on API endpoints. Runs after JwtAuthenticationFilter which sets
 * the X-User-Role header.
 *
 * Role hierarchy: ADMIN > ORGANIZER > USER
 */
@Component
public class RbacAuthorizationFilter implements GlobalFilter, Ordered {

    // Maps path prefixes to minimum required roles
    private static final List<RoleRule> ROLE_RULES = List.of(
            // Admin-only endpoints
            new RoleRule("POST", "/api/v1/admin/", List.of("ADMIN")),
            new RoleRule("DELETE", "/api/v1/admin/", List.of("ADMIN")),

            // Organizer endpoints — create/update/delete events, venues, layouts, sessions
            new RoleRule("POST", "/api/v1/events", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("PUT", "/api/v1/events/", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("DELETE", "/api/v1/events/", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("POST", "/api/v1/venues", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("PUT", "/api/v1/venues/", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("DELETE", "/api/v1/venues/", List.of("ORGANIZER", "ADMIN")),

            // Finance endpoints — organizer and admin only
            new RoleRule("GET", "/api/v1/finance/", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("POST", "/api/v1/finance/", List.of("ORGANIZER", "ADMIN")),
            new RoleRule("PUT", "/api/v1/finance/", List.of("ORGANIZER", "ADMIN"))
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String userRole = request.getHeaders().getFirst("X-User-Role");

        // If no role header, the request either didn't need auth or auth already failed
        if (userRole == null || userRole.isBlank()) {
            return chain.filter(exchange);
        }

        // Check each RBAC rule
        for (RoleRule rule : ROLE_RULES) {
            if (method.equals(rule.method) && path.startsWith(rule.pathPrefix)) {
                if (!rule.allowedRoles.contains(userRole)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                break; // First matching rule wins
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90; // After JwtAuthenticationFilter (-100)
    }

    private record RoleRule(String method, String pathPrefix, List<String> allowedRoles) {}
}
