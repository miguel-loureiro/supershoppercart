package com.supershoppercart.security.filter;

import com.supershoppercart.models.Shopper;
import com.supershoppercart.repositories.ShopperRepository;
import com.supershoppercart.services.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Filter to handle JWT authentication.
 * It extracts the JWT from the Authorization header, validates it,
 * and sets the authenticated shopper in the SecurityContext.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final ShopperRepository shopperRepository;
    private final JwtTokenService jwtTokenService;

    // Use constructor injection for dependencies
    public JwtAuthFilter(ShopperRepository shopperRepository, JwtTokenService jwtTokenService) {
        this.shopperRepository = shopperRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Log the Authorization header to verify its presence and format
        if (authHeader == null) {
            System.out.println("Authorization header is missing.");
        } else {
            System.out.println("Authorization header found: " + authHeader);
        }

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return; // Exit early if no token or invalid format
        }

        String jwt = authHeader.substring(7); // Extract the JWT token
        System.out.println("Extracted JWT: " + jwt);

        // Check if a user is already authenticated in the current security context
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 1. Validate the JWT token
                if (jwtTokenService.isTokenValid(jwt)) {
                    // 2. Extract shopper ID from the token
                    String shopperId = jwtTokenService.extractShopperId(jwt);

                    if (shopperId != null) {
                        // 3. Retrieve Shopper from the repository using the extracted ID
                        Optional<Shopper> shopperOptional = shopperRepository.findById(shopperId);

                        if (shopperOptional.isPresent()) {
                            Shopper shopper = shopperOptional.get();

                            // 4. Create an authentication token
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(shopper, null, Collections.emptyList());

                            // Set details from the request (e.g., remote address)
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // 5. Set the authentication in the SecurityContext
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("Authenticated Shopper with ID: " + shopper.getId());
                        } else {
                            System.out.println("Shopper not found for ID: " + shopperId);
                            // Explicitly send an error for not found user
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Shopper not found for token.");
                            return;
                        }
                    } else {
                        System.out.println("Shopper ID could not be extracted from token.");
                        // Explicitly send an error for un-extractable ID
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token payload.");
                        return;
                    }
                } else {
                    System.out.println("Invalid JWT token: " + jwt);
                    // Explicitly send an error for invalid token
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

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
