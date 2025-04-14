package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AuthToken {
	private static final long EXPIRATION_TIME = 7200000; // 2 hours in milliseconds
	private static final long REFRESH_THRESHOLD = 300000; // 5 minutes in milliseconds

	private String username;
	private String role;
	private String tokenID;
	private Instant creationTime;
	private Instant expirationTime;
	private boolean isRevoked;
	private String lastAccessIP;

	public AuthToken() {
		this.isRevoked = false;
	}

	public AuthToken(String username, String role, String clientIP) {
		this();
		this.username = Objects.requireNonNull(username, "Username cannot be null");
		this.role = Objects.requireNonNull(role, "Role cannot be null");
		this.tokenID = UUID.randomUUID().toString();
		this.creationTime = Instant.now();
		this.expirationTime = this.creationTime.plusMillis(EXPIRATION_TIME);
		this.lastAccessIP = clientIP;
	}

	public boolean isValid() {
		return !isRevoked && Instant.now().isBefore(expirationTime);
	}

	public boolean needsRefresh() {
		return !isRevoked && 
			   Instant.now().plusMillis(REFRESH_THRESHOLD).isAfter(expirationTime);
	}

	public void refresh() {
		if (!isRevoked) {
			this.expirationTime = Instant.now().plusMillis(EXPIRATION_TIME);
		}
	}

	public void revoke() {
		this.isRevoked = true;
	}

	// Getters
	public String getUsername() { return username; }
	public String getRole() { return role; }
	public String getTokenID() { return tokenID; }
	public Instant getCreationTime() { return creationTime; }
	public Instant getExpirationTime() { return expirationTime; }
	public boolean isRevoked() { return isRevoked; }
	public String getLastAccessIP() { return lastAccessIP; }

	public void updateLastAccessIP(String ip) {
		this.lastAccessIP = ip;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AuthToken)) return false;
		AuthToken that = (AuthToken) o;
		return Objects.equals(tokenID, that.tokenID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tokenID);
	}

	public void setTokenID(String tokenID) { this.tokenID = tokenID; }
	public void setUsername(String username) { this.username = username; }
	public void setRole(String role) { this.role = role; }
	public void setCreationTime(long creationTime) { this.creationTime = Instant.ofEpochMilli(creationTime); }
	public void setExpirationTime(long expirationTime) { this.expirationTime = Instant.ofEpochMilli(expirationTime); }
}