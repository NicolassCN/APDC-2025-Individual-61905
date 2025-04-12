package pt.unl.fct.di.apdc.indiv.util;

import java.util.Date;
import java.util.UUID;

import org.conscrypt.ct.DigitallySigned.SignatureAlgorithm;

import io.jsonwebtoken.Jwts;

public class AuthToken {
    private String username;
    private String role;
    private String validFrom;
    private String validTo;
    private String verifier;
    private String token;

    public AuthToken() {}

    public AuthToken(String username, String role) {
        this.username = username;
        this.role = role;
        this.validFrom = new Date().toInstant().toString();
        this.validTo = new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000).toInstant().toString(); // 2 hours
        this.verifier = UUID.randomUUID().toString();
        this.token = generateJwt();
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public String getVerifier() {
        return verifier;
    }

    public String getToken() {
        return token;
    }

    private String generateJwt() {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("verifier", verifier)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, "your-secure-secret-key") // Replace with secure key
                .compact();
    }

    public static AuthToken validate(String token) {
        try {
            var claims = Jwts.parser().setSigningKey("your-secure-secret-key").parseClaimsJws(token).getBody();
            AuthToken authToken = new AuthToken();
            authToken.username = claims.getSubject();
            authToken.role = claims.get("role", String.class);
            authToken.verifier = claims.get("verifier", String.class);
            authToken.validFrom = claims.getIssuedAt().toInstant().toString();
            authToken.validTo = claims.getExpiration().toInstant().toString();
            authToken.token = token;
            return authToken;
        } catch (Exception e) {
            return null;
        }
    }
}