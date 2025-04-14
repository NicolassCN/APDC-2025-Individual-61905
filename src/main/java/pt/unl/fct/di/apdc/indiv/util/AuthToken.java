package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.UUID;

public class AuthToken {
	private final String tokenId;
	private final String username;
	private final User.Role role;
	private final long creationTime;
	private final long expirationTime;
	private static final long TOKEN_EXPIRATION = 2 * 60 * 60 * 1000; // 2 hours in milliseconds

	public AuthToken(String username, User.Role role) {
		this.tokenId = UUID.randomUUID().toString();
		this.username = username;
		this.role = role;
		this.creationTime = Instant.now().toEpochMilli();
		this.expirationTime = this.creationTime + TOKEN_EXPIRATION;
	}

	public String getTokenId() {
		return tokenId;
	}

	public String getUsername() {
		return username;
	}

	public User.Role getRole() {
		return role;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public boolean isValid() {
		return Instant.now().toEpochMilli() < expirationTime;
	}
}