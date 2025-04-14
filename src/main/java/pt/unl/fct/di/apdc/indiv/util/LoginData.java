package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;

public class LoginData {
	private String username;
	private String password;
	private String clientIP;
	private String userAgent;
	private Instant loginAttemptTime;
	private int attemptCount;

	public LoginData() {
		this.loginAttemptTime = Instant.now();
		this.attemptCount = 0;
	}

	public LoginData(String username, String password, String clientIP, String userAgent) {
		this();
		this.username = Objects.requireNonNull(username, "Username cannot be null");
		this.password = Objects.requireNonNull(password, "Password cannot be null");
		this.clientIP = clientIP;
		this.userAgent = userAgent;
	}

	public boolean isValid() {
		return username != null && !username.isBlank() &&
			   password != null && !password.isBlank();
	}

	public void incrementAttempt() {
		this.attemptCount++;
	}

	// Getters
	public String getUsername() { return username; }
	public String getPassword() { return password; }
	public String getClientIP() { return clientIP; }
	public String getUserAgent() { return userAgent; }
	public Instant getLoginAttemptTime() { return loginAttemptTime; }
	public int getAttemptCount() { return attemptCount; }

	@Override
	public String toString() {
		return String.format("LoginAttempt[user=%s, time=%s, ip=%s]", 
			username, loginAttemptTime, clientIP);
	}
}
