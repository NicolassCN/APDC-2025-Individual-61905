package pt.unl.fct.di.apdc.indiv.util;

import java.util.UUID;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class AuthToken {
    public static final String TOKEN_KIND = "Token";
    // Token expiration time in milliseconds (24 hours)
    public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24h

    private String username;
    private String role;
    private String tokenID;
    private long creationDate;
    private long expirationDate;
    private String verifier;

    public AuthToken() {
    }

    public AuthToken(String username, String role) {
        this.username = username;
        this.role = role;
        this.tokenID = UUID.randomUUID().toString();
        this.creationDate = System.currentTimeMillis();
        this.expirationDate = this.creationDate + EXPIRATION_TIME;
        this.verifier = UUID.randomUUID().toString();
    }

    public boolean isValid() {
        return System.currentTimeMillis() < expirationDate;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationDate;
    }

    public void invalidate() {
        this.expirationDate = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTokenID() { return tokenID; }
    public void setTokenID(String tokenID) { this.tokenID = tokenID; }

    public long getCreationDate() { return creationDate; }
    public void setCreationDate(long creationDate) { this.creationDate = creationDate; }

    public long getExpirationDate() { return expirationDate; }
    public void setExpirationDate(long expirationDate) { this.expirationDate = expirationDate; }

    public String getVerifier() { return verifier; }
    public void setVerifier(String verifier) { this.verifier = verifier; }

    public Entity toEntity(Datastore datastore) {
        Key key = datastore.newKeyFactory().setKind(TOKEN_KIND).newKey(tokenID);
        return Entity.newBuilder(key)
                .set("username", username)
                .set("role", role)
                .set("creationDate", creationDate)
                .set("expirationDate", expirationDate)
                .set("verifier", verifier)
                .build();
    }

    public static AuthToken fromEntity(Entity entity) {
        AuthToken token = new AuthToken();
        token.tokenID = entity.getKey().getName();
        token.username = entity.getString("username");
        token.role = entity.getString("role");
        token.creationDate = entity.getLong("creationDate");
        token.expirationDate = entity.getLong("expirationDate");
        token.verifier = entity.getString("verifier");
        return token;
    }
}