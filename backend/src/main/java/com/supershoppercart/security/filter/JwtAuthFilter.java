package com.supershoppercart.security.filter;

import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.services.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final ShopperRepository shopperRepository;
    private final JwtTokenService jwtTokenService;
    private final Environment environment;              // 1. Add Spring Environment

    private static final String DEV_MAGIC_TOKEN = "DEV_MAGIC_TOKEN";
    private static final String DEV_PROFILE = "dev";    // or "local" as needed

    public JwtAuthFilter(
            ShopperRepository shopperRepository,
            JwtTokenService jwtTokenService,
            Environment environment                      // 2. Add to constructor
    ) {
        this.shopperRepository = shopperRepository;
        this.jwtTokenService = jwtTokenService;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Print header status for debugging.
        if (authHeader == null) {
            System.out.println("Authorization header is missing.");
        } else {
            System.out.println("Authorization header found: " + authHeader);
        }

        // 1. Check for the DEV_MAGIC_TOKEN in the 'dev' profile.
        if (authHeader != null && authHeader.equals("Bearer " + DEV_MAGIC_TOKEN) && isDevProfileActive()) {
            // Shortcut: create/dev Shopper principal and authenticate.
            Shopper devShopper = createDevShopper();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(devShopper, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("Authenticated DEV_MAGIC_TOKEN user in DEV mode.");

            filterChain.doFilter(request, response);
            return; // Bypass normal JWT logic.
        }

        // 2. If it's a 'Bearer' token but not the DEV token, handle it as a standard JWT.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            System.out.println("Extracted JWT: " + jwt);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    if (jwtTokenService.isTokenValid(jwt)) {
                        String shopperId = jwtTokenService.extractShopperId(jwt);
                        if (shopperId != null) {
                            Optional<Shopper> shopperOptional = shopperRepository.findById(shopperId);
                            if (shopperOptional.isPresent()) {
                                Shopper shopper = shopperOptional.get();
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(shopper, null, Collections.emptyList());
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                System.out.println("Authenticated Shopper with ID: " + shopper.getId());
                            } else {
                                System.out.println("Shopper not found for ID: " + shopperId);
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Shopper not found for token.");
                                return;
                            }
                        } else {
                            System.out.println("Shopper ID could not be extracted from token.");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token payload.");
                            return;
                        }
                    } else {
                        System.out.println("Invalid JWT token: " + jwt);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token.");
                        return;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    System.err.println("Error during authentication process: " + e.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed due to data access error.");
                    return;
                } catch (Exception e) {
                    System.err.println("Unexpected error during token processing: " + e.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token processing error.");
                    return;
                }
            }
        }

        // Continue the filter chain for all other cases (e.g., no auth header, or successfully authenticated JWT).
        filterChain.doFilter(request, response);
    }

    // Helper to check if running in dev profile
    private boolean isDevProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains(DEV_PROFILE);
    }

    // Helper to create a mock dev user
    private Shopper createDevShopper() {
        Shopper shopper = new Shopper();
        shopper.setId("dev-shopper-id");
        shopper.setEmail("devuser@example.com");
        // Set other dev attributes as needed
        return shopper;
    }
}
