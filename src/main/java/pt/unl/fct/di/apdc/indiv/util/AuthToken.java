package pt.unl.fct.di.apdc.indiv.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2;
	
	private String username;
	private String role;
	private long creationData;
	private long expirationData;
	private String tokenID;
	
	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + EXPIRATION_TIME;
		this.tokenID = UUID.randomUUID().toString();
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getRole() {
		return role;
	}
	
	public long getCreationData() {
		return creationData;
	}
	
	public long getExpirationData() {
		return expirationData;
	}
	
	public String getTokenID() {
		return tokenID;
	}
	
	public boolean isValid() {
		return System.currentTimeMillis() <= expirationData;
	}
}