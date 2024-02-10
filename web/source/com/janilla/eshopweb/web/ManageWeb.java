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
package com.janilla.eshopweb.web;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class ManageWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/manage/my-account")
	public Object showProfile(HttpExchange context) throws IOException {
		var u = ((CustomHttpExchange) context).getUser();
		if (u == null)
			return URI.create("/Identity/Account/Login?ReturnUrl=%2Fmanage%2Fmy-account");
		var m = Profile.statusMessages.remove(u.getId());
		return new MyAccount(new Profile(m, u, null));
	}

	static Pattern emailPattern = Pattern.compile("\\S+@\\S+\\.\\S+");

	static Pattern phonePattern = Pattern.compile("\\+?[0-9 \\.\\-\\(\\)]+");

	@Handle(method = "POST", uri = "/manage/my-account")
	public Object saveProfile(ApplicationUser user, HttpExchange context) throws IOException {
		var u = ((CustomHttpExchange) context).getUser();

		Map<String, String> m = new LinkedHashMap<>();
		if (user.getEmail().isBlank())
			m.put("email", "The Email field is required.");
		else if (!emailPattern.matcher(user.getEmail()).matches())
			m.put("email", "The Email field is not a valid e-mail address.");
		if (!user.getPhoneNumber().isBlank() && !phonePattern.matcher(user.getPhoneNumber()).matches())
			m.put("phoneNumber", "The Phone number field is not a valid phone number.");
		if (!m.isEmpty()) {
			user.setUserName(u.getUserName());
			return new MyAccount(new Profile(null, user, m));
		}

		var c = persistence.getCrud(ApplicationUser.class);
		persistence.getDatabase().performTransaction(
				() -> c.update(u.getId(), w -> Reflection.copy(user, w, n -> Set.of("username", "email").contains(n))));
		Profile.statusMessages.put(u.getId(), "Your profile has been updated");
		return URI.create("/manage/my-account");
	}

	@Handle(method = "POST", uri = "/manage/send-verification-email")
	public Object sendVerificationEmail(HttpExchange context) throws IOException {
		var u = ((CustomHttpExchange) context).getUser();
		Profile.statusMessages.put(u.getId(), "Verification email sent. Please check your email.");
		return URI.create("/manage/my-account");
	}

	@Handle(method = "GET", uri = "/manage/change-password")
	public Object showChangePassword(HttpExchange context) throws IOException {
		var u = ((CustomHttpExchange) context).getUser();
		if (u == null)
			return URI.create("/Identity/Account/Login?ReturnUrl=%2Fmanage%2Fchange-password");
		var m = ChangePassword.statusMessages.remove(u.getId());
		return new MyAccount(new ChangePassword(m, null, null));
	}

	@Handle(method = "POST", uri = "/manage/change-password")
	public Object saveChangePassword(ChangePassword.Form form, HttpExchange context) throws IOException {
		var u = ((CustomHttpExchange) context).getUser();

		Map<String, String> m = new LinkedHashMap<>();
		if (form.oldPassword.isBlank())
			m.put("oldPassword", "The Current password field is required.");
		if (form.newPassword.isBlank())
			m.put("newPassword", "The New password field is required.");
		else if (form.newPassword.length() < 6 || form.newPassword.length() > 100)
			m.put("newPassword", "The New password must be at least 6 and at max 100 characters long.");
		if (!form.confirmPassword.equals(form.newPassword))
			m.put("confirmPassword", "The new password and confirmation password do not match.");
		if (m.isEmpty() && !ApplicationUser.testPassword(form.oldPassword, u))
			m.put("oldPassword", "Incorrect password.");
		if (!m.isEmpty())
			return new MyAccount(new ChangePassword(null, form, m));

		var c = persistence.getCrud(ApplicationUser.class);
		persistence.getDatabase().performTransaction(
				() -> c.update(u.getId(), w -> ApplicationUser.setHashAndSalt(w, form.newPassword)));
		ChangePassword.statusMessages.put(u.getId(), "Your password has been changed.");
		return URI.create("/manage/change-password");
	}

	@Render(template = "MyAccount.html")
	public record MyAccount(Object content) {

		public Collection<NavItem> navItems() {
			return List.of(new NavItem(URI.create("/manage/my-account"), "Profile", content instanceof Profile),
					new NavItem(URI.create("/manage/change-password"), "Password", content instanceof ChangePassword),
					new NavItem(URI.create("/manage/two-factor-authentication"), "Two-factor authentication",
							content instanceof TwoFactorAuthentication));
		}

		@Render(template = "MyAccount-NavItem.html")
		public record NavItem(URI uri, String text, boolean current) {

			public String activeClass() {
				return current ? "active" : null;
			}
		}
	}

	@Render(template = "Profile.html")
	public record Profile(@Render(template = "MyAccount-statusMessage.html") String statusMessage, ApplicationUser user,
			Map<String, @Render(template = "MyAccount-validationMessage.html") String> validationMessages) {

		static Map<Long, String> statusMessages = new ConcurrentHashMap<>();

		@Render(template = "MyAccount-validationSummary.html")
		public Collection<@Render(template = """
				<li>${}</li>
				""") String> validationSummary() {
			return validationMessages != null ? validationMessages.values() : null;
		}
	}

	@Render(template = "ChangePassword.html")
	public record ChangePassword(@Render(template = "MyAccount-statusMessage.html") String statusMessage, Form form,
			Map<String, @Render(template = "MyAccount-validationMessage.html") String> validationMessages) {

		static Map<Long, String> statusMessages = new ConcurrentHashMap<>();

		@Render(template = "MyAccount-validationSummary.html")
		public Collection<@Render(template = """
				<li>${}</li>
				""") String> validationSummary() {
			return validationMessages != null ? validationMessages.values() : null;
		}

		public record Form(String oldPassword, String newPassword, String confirmPassword) {
		}
	}

	@Render(template = "TwoFactorAuthentication.html")
	public record TwoFactorAuthentication(@Render(template = "MyAccount-statusMessage.html") String statusMessage,
			ApplicationUser user,
			Map<String, @Render(template = "MyAccount-validationMessage.html") String> validationMessages) {

		static Map<Long, String> statusMessages = new ConcurrentHashMap<>();

		@Render(template = "MyAccount-validationSummary.html")
		public Collection<@Render(template = """
				<li>${}</li>
				""") String> validationSummary() {
			return validationMessages != null ? validationMessages.values() : null;
		}
	}
}
