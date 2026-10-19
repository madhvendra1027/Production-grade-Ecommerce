package com.example.project.security;

import com.example.project.security.jwt.AuthEntryPointJwt;
import com.example.project.security.jwt.AuthTokenFilter;
import com.example.project.security.services.UserDetailsServiceImplementation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
// Enabled so @PreAuthorize on service/controller methods is actually honoured.
// Previously commented out, which meant any @PreAuthorize annotation would have
// been silently ignored.
@EnableMethodSecurity
public class WebSecurityConfiguration {

    @Value("${frontend.url:http://localhost:5173}")
    private String frontEndUrl;

    @Autowired
    UserDetailsServiceImplementation userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> patterns = new ArrayList<>(List.of(
                "https://*.vercel.app",
                "https://*.onrender.com",
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));
        if (frontEndUrl != null && !frontEndUrl.trim().isEmpty()) {
            Arrays.stream(frontEndUrl.split(","))
                    .map(String::trim)
                    .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                    .filter(url -> !url.isEmpty() && !patterns.contains(url))
                    .forEach(patterns::add);
        }
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF note: the JWT travels in a cookie, which makes this app a CSRF target.
        // The protection currently relied upon is the cookie's SameSite attribute
        // (see JwtUtils / app.jwt.cookie-same-site, default Lax), which stops browsers
        // from attaching the cookie to cross-site POST/PUT/DELETE requests.
        // If you ever need SameSite=None - e.g. frontend and API on different
        // registrable domains - you MUST re-enable CSRF tokens here, because SameSite
        // will no longer be doing that job.
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- genuinely public ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/api/auth/signin", "/api/auth/signup").permitAll()
                        // signout stays open so an expired session can still clear its cookie
                        .requestMatchers("/api/auth/signout").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // API docs. Disable in production via springdoc.api-docs.enabled=false
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // --- role gated ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/seller/**").hasAnyRole("ADMIN", "SELLER")

                        // Everything else, including /api/auth/user and /api/auth/username,
                        // requires a valid token. Those two read
                        // authentication.getPrincipal() and used to NPE into a 500 when
                        // called anonymously because /api/auth/** was blanket-permitAll.
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Removed: the WebSecurityCustomizer that bypassed the filter chain entirely for
    // a list of legacy springfox paths, and the /h2-console/** + /api/test/**
    // permitAll rules. The H2 console is no longer enabled (this app runs on
    // Postgres) and frameOptions no longer needs relaxing for it.

    // Removed: the initData CommandLineRunner that seeded admin/adminPass,
    // seller1/password2 and user1/password1 on every boot. Role rows are now
    // bootstrapped by configuration.DataInitializer, and optional demo accounts
    // live in configuration.DemoDataInitializer behind an explicit opt-in flag
    // with passwords supplied from the environment.
}
