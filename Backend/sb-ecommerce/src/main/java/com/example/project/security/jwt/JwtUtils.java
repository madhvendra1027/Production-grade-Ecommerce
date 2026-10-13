package com.example.project.security.jwt;

import com.example.project.security.services.UserDetailsImplementation;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtExpirationInMs}")
    private Long jwtExpirationInMs;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String JwtCookie;

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    /**
     * Send the cookie only over HTTPS. Must be true in any real deployment;
     * defaults to false so plain-HTTP localhost development still works.
     */
    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    /**
     * SameSite is what stands in for CSRF protection here - see the note in
     * WebSecurityConfiguration. "Lax" blocks the cookie on cross-site
     * POST/PUT/DELETE while keeping normal top-level navigation working.
     * Only use "None" if the frontend is on a different registrable domain, and
     * re-enable CSRF tokens if you do.
     */
    @Value("${app.jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    public String getJwtFromCookies(HttpServletRequest request){
        Cookie cookie = WebUtils.getCookie(request, JwtCookie);
        if(cookie != null){
            return cookie.getValue();
        }
        else{
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImplementation userPrinciple){
        String jwt = getTokenFromUsername(userPrinciple.getUsername());
        return ResponseCookie.from(JwtCookie, jwt)
                .path("/api")
                // httpOnly was false, which let any script on the page - including
                // injected ones - read the session token straight out of document.cookie.
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                // Previously hardcoded to 24h while the token itself expired after
                // jwtExpirationInMs (~50 min), so the browser kept sending a cookie
                // the server had already stopped accepting. Now they match exactly.
                .maxAge(Duration.ofMillis(jwtExpirationInMs))
                .build();
    }

    public ResponseCookie getCleanJwtCookie(){
        // maxAge(0) is what actually removes the cookie. The old version passed a
        // null value with no maxAge, which left the cookie in place in some browsers.
        // Every other attribute must match the cookie being replaced or it will not
        // be overwritten.
        return ResponseCookie.from(JwtCookie, "")
                .path("/api")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(0)
                .build();
    }

    //Generating token from username
    public String getTokenFromUsername(String username){
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime() + jwtExpirationInMs)))
                .signWith(key())
                .compact();
    }

    //    Generating JWT tokens from HEADER
    public String getJwtFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        // Do not log the header: it contains the bearer token verbatim, and this
        // class logs at DEBUG.
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7); //to remove bearer prefix
        }
        return null;
    }

    //Getting username from Jwt
    public String getUserNameFromToken(String token){
        return Jwts.parser()
                   .verifyWith(key())
                   .build().parseSignedClaims(token) //claims --> contents inside the token
                   .getPayload().getSubject();
    }

    /**
     * Generate signing key. The secret is expected to be Base64 encoded and must
     * decode to at least 256 bits; jjwt rejects anything weaker for HS256.
     */
    public SecretKey key(){
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    //Validating JWT tokens
    public boolean validateJwtToken(String authToken){
        try{
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(authToken);

            return true;
        }catch(MalformedJwtException exception){
            logger.error("Invalid JWT token : {}", exception.getMessage());
        }catch(ExpiredJwtException exception){
            logger.debug("JWT token is expired : {}", exception.getMessage());
        }catch(UnsupportedJwtException exception){
            logger.error("JWT token is unsupported : {}", exception.getMessage());
        }catch(IllegalArgumentException exception){
            logger.error("JWT claims string is empty : {}", exception.getMessage());
        }catch(JwtException exception){
            // Parent of the jjwt exception hierarchy, so this also covers signature
            // verification failures (a forged or tampered token). Caught by the
            // supertype because the concrete SignatureException class has moved
            // package between jjwt releases.
            logger.error("JWT token rejected : {}", exception.getMessage());
        }
        return false;
    }
}
