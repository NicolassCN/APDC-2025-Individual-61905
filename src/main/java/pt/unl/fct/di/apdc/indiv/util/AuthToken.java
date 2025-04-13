package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class AuthToken {

	private String user;
	private String role;
	private TokenValidity validity;
	
	public static class TokenValidity {
		private String valid_from;
		private String valid_to;
		private String verificador;
		
		public TokenValidity() {
			Instant now = Instant.now();
			this.valid_from = now.toString();
			this.valid_to = now.plus(2, ChronoUnit.HOURS).toString();
			this.verificador = UUID.randomUUID().toString();
		}
		
		public String getValid_from() {
			return valid_from;
		}
		
		public String getValid_to() {
			return valid_to;
		}
		
		public String getVerificador() {
			return verificador;
		}
		
		public boolean isValid() {
			Instant now = Instant.now();
			Instant validTo = Instant.parse(valid_to);
			return now.isBefore(validTo);
		}
	}
	
	public AuthToken(String user, String role) {
		this.user = user;
		this.role = role;
		this.validity = new TokenValidity();
	}
	
	public String getUser() {
		return user;
	}
	
	public String getRole() {
		return role;
	}
	
	public TokenValidity getValidity() {
		return validity;
	}
	
	public boolean isValid() {
		return validity.isValid();
	}
	
	public String getTokenID() {
		return validity.getVerificador();
	}
}