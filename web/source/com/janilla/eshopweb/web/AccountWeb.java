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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class AccountWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "/account/profile")
	public Account getProfile(HttpExchange exchange) throws IOException {
		var u = ((CustomHttpExchange) exchange).getUser(true);
		var m = Profile.statusMessages.remove(u.getId());
		return new Account(new Profile(m, u, null));
	}

	static Pattern phonePattern = Pattern.compile("\\+?[0-9 \\.\\-\\(\\)]+");

	@Handle(method = "POST", path = "/account/profile")
	public Object updateProfile(ApplicationUser user, HttpExchange exchange) throws IOException {
		var u = ((CustomHttpExchange) exchange).getUser(true);

		var v = new EntryList<String, String>();
		if (user.getEmail().isBlank())
			v.set("email", "The Email field is required.");
		else if (!UserWeb.emailPattern.matcher(user.getEmail()).matches())
			v.set("email", "The Email field is not a valid e-mail address.");
		if (!user.getPhoneNumber().isBlank() && !phonePattern.matcher(user.getPhoneNumber()).matches())
			v.set("phoneNumber", "The Phone number field is not a valid phone number.");
		if (!v.isEmpty()) {
			user.setUserName(u.getUserName());
			return new Account(new Profile(null, user, v));
		}

		var c = persistence.getCrud(ApplicationUser.class);
		c.update(u.getId(), w -> Reflection.copy(user, w, n -> Set.of("username", "email").contains(n)));
		Profile.statusMessages.put(u.getId(), "Your profile has been updated");
		return URI.create("/account/profile");
	}

	@Handle(method = "POST", path = "/account/verification")
	public URI sendVerificationEmail(HttpExchange exchange) throws IOException {
		var u = ((CustomHttpExchange) exchange).getUser(true);
		Profile.statusMessages.put(u.getId(), "Verification email sent. Please check your email.");
		return URI.create("/account/profile");
	}

	@Handle(method = "GET", path = "/account/password")
	public Account getPassword(HttpExchange exchange) throws IOException {
		var u = ((CustomHttpExchange) exchange).getUser(true);
		var m = Password.statusMessages.remove(u.getId());
		return new Account(new Password(m, null, null));
	}

	@Handle(method = "POST", path = "/account/password")
	public Object changePassword(Password.Form form, HttpExchange exchange) throws IOException {
		var u = ((CustomHttpExchange) exchange).getUser(true);

		var v = new EntryList<String, String>();
		if (form.oldPassword.isBlank())
			v.set("oldPassword", "The Current password field is required.");
		if (form.newPassword.isBlank())
			v.set("newPassword", "The New password field is required.");
		else if (form.newPassword.length() < 6 || form.newPassword.length() > 100)
			v.set("newPassword", "The New password must be at least 6 and at max 100 characters long.");
		if (!form.confirmPassword.equals(form.newPassword))
			v.set("confirmPassword", "The new password and confirmation password do not match.");
		if (v.isEmpty() && !ApplicationUser.testPassword(form.oldPassword, u))
			v.set("oldPassword", "Incorrect password.");
		if (!v.isEmpty())
			return new Account(new Password(null, form, v));

		var c = persistence.getCrud(ApplicationUser.class);
		c.update(u.getId(), w -> ApplicationUser.setHashAndSalt(w, form.newPassword));
		Password.statusMessages.put(u.getId(), "Your password has been changed.");
		return URI.create("/manage/change-password");
	}

	@Render(template = "Account.html")
	public record Account(Page page) implements Page {

		public List<NavItem> navItems() {
			return List.of(new NavItem(URI.create("/account/profile"), "Profile", page instanceof Profile),
					new NavItem(URI.create("/account/password"), "Password", page instanceof Password),
					new NavItem(URI.create("/account/authenticator"), "Two-factor authentication",
							page.getClass().getEnclosingClass().equals(TwoFactorAuthenticationWeb.class)));
		}

		@Override
		public String title() {
			return page.title();
		}

		@Render(template = "Account-NavItem.html")
		public record NavItem(URI uri, String text, boolean current) {

			public String activeClass() {
				return current ? "active" : null;
			}
		}
	}

	@Render(template = "Profile.html")
	public record Profile(@Render(template = "StatusMessage.html") String statusMessage, ApplicationUser user,
			EntryList<String, @Render(template = "ValidationMessage.html") String> validationMessages) implements Page {

		static Map<Long, String> statusMessages = new ConcurrentHashMap<>();

		@Override
		public String title() {
			return "Profile";
		}

		@Render(template = "ValidationSummary.html")
		public Stream<@Render(template = """
				<li>____</li>
				""") String> validationSummary() {
			return validationMessages != null && !validationMessages.isEmpty()
					? validationMessages.stream().map(Entry::getValue)
					: null;
		}
	}

	@Render(template = "Password.html")
	public record Password(@Render(template = "StatusMessage.html") String statusMessage, Form form,
			EntryList<String, @Render(template = "ValidationMessage.html") String> validationMessages) implements Page {

		static Map<Long, String> statusMessages = new ConcurrentHashMap<>();

		@Override
		public String title() {
			return "Change password";
		}

		@Render(template = "ValidationSummary.html")
		public Stream<@Render(template = """
				<li>____</li>
				""") String> validationSummary() {
			return validationMessages != null && !validationMessages.isEmpty()
					? validationMessages.stream().map(Entry::getValue)
					: null;
		}

		public record Form(String oldPassword, String newPassword, String confirmPassword) {
		}
	}
}
