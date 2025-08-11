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

@Component // Mark as a Spring component for detection and injection
public class JwtAuthFilter extends OncePerRequestFilter { // Renamed from JwtAuthenticationFilter

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

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return; // Exit early if no token or invalid format
        }

        String jwt = authHeader.substring(7); // Extract the JWT token

        // Check if a user is already authenticated in the current security context
        // This prevents unnecessary processing if authentication has already happened
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
                            // For a simple authentication, authorities can be empty or derived from Shopper roles
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(shopper, null, Collections.emptyList());

                            // Set details from the request (e.g., remote address)
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // 5. Set the authentication in the SecurityContext
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("Authenticated Shopper with ID: " + shopper.getId());
                        } else {
                            System.out.println("Shopper not found for ID: " + shopperId);
                        }
                    } else {
                        System.out.println("Shopper ID could not be extracted from token.");
                    }
                } else {
                    System.out.println("Invalid JWT token: " + jwt);
                }
            } catch (ExecutionException | InterruptedException e) {
                // Log the exception
                System.err.println("Error during authentication process: " + e.getMessage());
                // Depending on your error handling strategy, you might want to
                // set a specific HTTP status code or rethrow a specific exception.
                // For now, rethrow as ServletException to let Spring handle it or GlobalExceptionHandler.
                throw new ServletException("Authentication failed due to data access error.", e);
            } catch (Exception e) { // Catch any other unexpected exceptions from token service
                System.err.println("Unexpected error during token processing: " + e.getMessage());
                throw new ServletException("Token processing error.", e);
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
