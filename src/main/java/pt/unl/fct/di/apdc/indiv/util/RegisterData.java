package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

public class RegisterData {
	private static final Logger LOG = Logger.getLogger(RegisterData.class.getName());
	
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
	private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{9,15}$");
	private static final int MIN_PASSWORD_LENGTH = 10;
	private static final int MIN_CRITERIA_REQUIRED = 3;
	
	private String username;
	private String password;
	private String confirmation;
	private String email;
	private String name;
	private String phone;
	private String profile;
	private Instant registrationTime;
	private String clientIP;
	private String userAgent;
	private String hashedPassword;

	public RegisterData() {
		this.registrationTime = Instant.now();
	}

	public RegisterData(String username, String password, String confirmation,
					   String email, String name, String phone, String profile,
					   String clientIP, String userAgent) {
		this();
		this.username = Objects.requireNonNull(username, "Username cannot be null");
		this.password = Objects.requireNonNull(password, "Password cannot be null");
		this.confirmation = Objects.requireNonNull(confirmation, "Confirmation cannot be null");
		this.email = Objects.requireNonNull(email, "Email cannot be null");
		this.name = Objects.requireNonNull(name, "Name cannot be null");
		this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
		this.profile = Objects.requireNonNull(profile, "Profile cannot be null");
		this.clientIP = clientIP;
		this.userAgent = userAgent;
	}

	public boolean validRegistration() {
		if (!validateBasicFields()) {
			LOG.warning("Basic field validation failed for user: " + username);
			return false;
		}

		if (!validateEmail()) {
			LOG.warning("Email validation failed for: " + email);
			return false;
		}

		if (!validatePhone()) {
			LOG.warning("Phone validation failed for: " + phone);
			return false;
		}

		if (!validatePasswords()) {
			LOG.warning("Password validation failed for user: " + username);
			return false;
		}

		if (!validateProfile()) {
			LOG.warning("Profile validation failed. Value: " + profile);
			return false;
		}

		// Hash password only after all validations pass
		this.hashedPassword = DigestUtils.sha512Hex(this.password);
		// Clear plain text password
		this.password = null;
		this.confirmation = null;

		return true;
	}

	private boolean validateBasicFields() {
		return nonEmptyOrBlankField(username) &&
			   nonEmptyOrBlankField(password) &&
			   nonEmptyOrBlankField(confirmation) &&
			   nonEmptyOrBlankField(email) &&
			   nonEmptyOrBlankField(name) &&
			   nonEmptyOrBlankField(phone) &&
			   nonEmptyOrBlankField(profile);
	}

	private boolean validateEmail() {
		return EMAIL_PATTERN.matcher(email).matches();
	}

	private boolean validatePhone() {
		return PHONE_PATTERN.matcher(phone).matches();
	}

	private boolean validatePasswords() {
		return password.equals(confirmation) && checkPasswordComplexity();
	}

	private boolean validateProfile() {
		return profile.equalsIgnoreCase("publico") || 
			   profile.equalsIgnoreCase("privado");
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	private boolean checkPasswordComplexity() {
		if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
			LOG.warning("Password too short for user: " + username);
			return false;
		}

		int criteriaCount = 0;
		if (password.matches(".*[A-Z].*")) criteriaCount++; // Uppercase
		if (password.matches(".*[a-z].*")) criteriaCount++; // Lowercase
		if (password.matches(".*\\d.*")) criteriaCount++;   // Digit
		if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) criteriaCount++; // Special

		boolean passed = criteriaCount >= MIN_CRITERIA_REQUIRED;
		if (!passed) {
			LOG.warning("Password complexity requirements not met for user: " + username);
		}
		return passed;
	}

	// Getters
	public String getUsername() { return username; }
	public String getEmail() { return email; }
	public String getName() { return name; }
	public String getPhone() { return phone; }
	public String getProfile() { return profile; }
	public Instant getRegistrationTime() { return registrationTime; }
	public String getClientIP() { return clientIP; }
	public String getUserAgent() { return userAgent; }
	public String getHashedPassword() { return hashedPassword; }

	@Override
	public String toString() {
		return String.format("Registration[user=%s, email=%s, time=%s]",
			username, email, registrationTime);
	}
}