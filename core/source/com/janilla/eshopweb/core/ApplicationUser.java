/*
 * MIT License
 *
 * Copyright (c) 2024 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.eshopweb.core;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Random;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

@Store
public class ApplicationUser {

	public static Random RANDOM = new SecureRandom();

	public static void setHashAndSalt(ApplicationUser user, String password) {
		var p = password.toCharArray();
		var s = new byte[16];
		RANDOM.nextBytes(s);
		var h = hash(p, s);
		var f = HexFormat.of();
		user.setPasswordHash(f.formatHex(h));
		user.setSalt(f.formatHex(s));
	}

	public static boolean testPassword(String password, ApplicationUser user) {
		var p = password.toCharArray();
		var f = HexFormat.of();
		var s = f.parseHex(user.getSalt());
		var h = f.formatHex(hash(p, s));
		return h.equals(user.getPasswordHash());
	}

	public static byte[] hash(char[] password, byte[] salt) {
		var s = new PBEKeySpec(password, salt, 10000, 512);
		try {
			var f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			return f.generateSecret(s).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private long id;

	private String userName;

	@Index
	private String email;

	private String phoneNumber;

	private String passwordHash;

	private String salt;

	private Collection<String> roles;

	private TwoFactor twoFactor;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public Collection<String> getRoles() {
		return roles;
	}

	public void setRoles(Collection<String> roles) {
		this.roles = roles;
	}

	public TwoFactor getTwoFactor() {
		return twoFactor;
	}

	public void setTwoFactor(TwoFactor twoFactor) {
		this.twoFactor = twoFactor;
	}

	public record TwoFactor(boolean enabled, String secretKey, Set<String> recoveryCodeHashes) {

		public TwoFactor withEnabled(boolean enabled) {
			return new TwoFactor(enabled, secretKey, recoveryCodeHashes);
		}

		public TwoFactor withSecretKey(String secretKey) {
			return new TwoFactor(enabled, secretKey, recoveryCodeHashes);
		}

		public TwoFactor withRecoveryCodeHashes(Set<String> recoveryCodeHashes) {
			return new TwoFactor(enabled, secretKey, recoveryCodeHashes);
		}
	}
}
