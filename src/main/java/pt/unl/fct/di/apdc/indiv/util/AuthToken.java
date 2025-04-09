package pt.unl.fct.di.apdc.indiv.util;

import java.util.UUID;

public class AuthToken {

	public String username;
	public String role;
	public long creationDate; // Timestamp in milliseconds
	public long expirationDate; // Timestamp in milliseconds
	public String tokenString; // Unique token identifier
	public String verifier; // Magic number for verification

	// Session validity time in milliseconds (e.g., 2 hours)
	public static final long EXPIRATION_TIME = 2 * 60 * 60 * 1000L; 

	public AuthToken() {
		// Default constructor for frameworks like GSON
	}

	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.creationDate = System.currentTimeMillis();
		this.expirationDate = this.creationDate + EXPIRATION_TIME;
		this.tokenString = UUID.randomUUID().toString(); // Simple UUID as token
		this.verifier = generateVerifier(); // Generate a magic number for verification
	}
	
	private String generateVerifier() {
		// Generate a random 6-digit number as verifier
		return String.format("%06d", (int)(Math.random() * 1000000));
	}

	// Getters are needed for serialization and access control checks
	public String getUsername() {
		return username;
	}

	public String getRole() {
		return role;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public long getExpirationDate() {
		return expirationDate;
	}

	public String getTokenString() {
		return tokenString;
	}
	
	public String getVerifier() {
		return verifier;
	}

	// Check if the token is still valid
	public boolean isValid() {
		return System.currentTimeMillis() < expirationDate;
	}
}