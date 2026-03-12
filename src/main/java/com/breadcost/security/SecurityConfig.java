package com.breadcost.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration — JWT stateless + Basic auth fallback for dev
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                // v1 public
                .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                // v2 public: customer register, login, catalog browse
                .requestMatchers(HttpMethod.POST, "/v2/customers/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/v2/customers/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/v2/customers/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/v2/customers/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/v2/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/v2/products").permitAll()
                // v3 public: WhatsApp webhook
                .requestMatchers(HttpMethod.POST, "/v3/ai/webhook/whatsapp").permitAll()
                // everything else under v1, v2, v3 requires authentication
                .requestMatchers("/v1/**").authenticated()
                .requestMatchers("/v2/**").authenticated()
                .requestMatchers("/v3/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(basic -> {})
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** In-memory demo users — Basic auth fallback for dev/Postman */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin").password(passwordEncoder.encode("admin")).roles("Admin").build();
        UserDetails production = User.builder()
                .username("production").password(passwordEncoder.encode("production")).roles("ProductionUser").build();
        UserDetails finance = User.builder()
                .username("finance").password(passwordEncoder.encode("finance")).roles("FinanceUser").build();
        UserDetails viewer = User.builder()
                .username("viewer").password(passwordEncoder.encode("viewer")).roles("Viewer").build();
        UserDetails cashier = User.builder()
                .username("cashier").password(passwordEncoder.encode("cashier")).roles("Cashier").build();
        UserDetails warehouse = User.builder()
                .username("warehouse").password(passwordEncoder.encode("warehouse")).roles("Warehouse").build();
        UserDetails technologist = User.builder()
                .username("technologist").password(passwordEncoder.encode("technologist")).roles("Technologist").build();
        UserDetails manager = User.builder()
                .username("manager").password(passwordEncoder.encode("manager")).roles("Manager").build();
        return new InMemoryUserDetailsManager(admin, production, finance, viewer, cashier, warehouse, technologist, manager);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
