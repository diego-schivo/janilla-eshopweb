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
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.core.Basket;
import com.janilla.eshopweb.core.BasketItem;
import com.janilla.json.Jwt;
import com.janilla.net.Net;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Parameter;
import com.janilla.reflect.Reflection;
import com.janilla.util.Base32;
import com.janilla.util.EntryList;
import com.janilla.util.Totp;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class UserWeb {

	Properties configuration;

	Persistence persistence;

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

//	static URI NAME_CLAIM_TYPE = URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name");
//
//	static URI ROLE_CLAIM_TYPE = URI.create("http://schemas.microsoft.com/ws/2008/06/identity/claims/role");

	@Handle(method = "GET", path = "/user")
	public Object getUser(EShopWebApp.Exchange exchange) {
		var u = exchange.getUser(false);
//		System.out.println("user=" + u);
		var t = getJwt(u.getEmail());
//		return new Current(u != null, NAME_CLAIM_TYPE, ROLE_CLAIM_TYPE, t,
//				u != null
//						? Stream.concat(Stream.of(new Claim(NAME_CLAIM_TYPE, u.getUserName())),
//								u.getRoles().stream().map(r -> new Claim(ROLE_CLAIM_TYPE, r))).toList()
//						: Collections.emptyList());
		return u != null ? Map.of("username", u.getUserName(), "roles", u.getRoles(), "token", t) : null;
	}

	protected String getJwt(String email) {
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("sub", email);
		return Jwt.generateToken(h, p, configuration.getProperty("eshopweb.jwt.key"));
	}

	@Handle(method = "GET", path = "/user/login")
	public Login getLogin() {
		return new Login(null, null);
	}

	static Pattern emailPattern = Pattern.compile("\\S+@\\S+\\.\\S+");

	@Handle(method = "POST", path = "/user/login")
	public Object authenticate(Login.Form form, @Parameter(name = "returnUrl") URI returnURI,
			EShopWebApp.Exchange exchange) throws IOException {
		var v = new ValidationMessages();
		if (form.email.isBlank())
			v.set("email", "The Email field is required.");
		else if (!emailPattern.matcher(form.email).matches())
			v.set("email", "The Email field is not a valid e-mail address.");
		if (form.password.isBlank())
			v.set("password", "The Password field is required.");

		ApplicationUser u = null;
		if (v.isEmpty()) {
			var c = persistence.getCrud(ApplicationUser.class);
			var i = c.find("email", form.email);
			u = i > 0 ? c.read(i) : null;
			if (u == null || !ApplicationUser.testPassword(form.password, u))
				v.set(null, "Invalid login attempt.");
		}

		if (!v.isEmpty())
			return new Login(form, v);

		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("sub", u.getEmail(), "twoFactorAuthenticated", false, "exp",
				Instant.now().getEpochSecond() + 10 * 60);
		var t = Jwt.generateToken(h, p, configuration.getProperty("eshopweb.jwt.key"));
		exchange.addUserCookie(t);

		if (u.getTwoFactor().enabled()) {
			var s = "/user/login/two-factor";
			if (returnURI != null) {
				var q = new EntryList<String, String>();
				q.add("returnUrl", returnURI.toString());
				s += "?" + Net.formatQueryString(q);
			}
			return URI.create(s);
		}

		var b = ((CustomHttpExchange) exchange).getBasket(false);
		if (b != null) {
			transferBasket(b, u);
			exchange.removeBasketCookie();
		}

		return returnURI != null ? returnURI : URI.create("/");
	}

	@Handle(method = "GET", path = "/user/login/two-factor")
	public TwoFactor getTwoFactor() {
		return new TwoFactor(null, null);
	}

	@Handle(method = "POST", path = "/user/login/two-factor")
	public Object authenticate(TwoFactor.Form form, @Parameter(name = "returnUrl") URI returnURI,
			EShopWebApp.Exchange exchange) throws IOException {
		var v = new ValidationMessages();
		if (form.code.isBlank())
			v.set("code", "The Code field is required.");

		ApplicationUser u = null;
		if (v.isEmpty()) {
			u = exchange.getUser(false);
			var c = Totp.getCode(Base32.decode(u.getTwoFactor().secretKey()));
//			System.out.println("c " + c);

			if (!c.equals(form.code))
				v.set(null, "Invalid login attempt.");
		}

		if (!v.isEmpty())
			return new TwoFactor(form, v);

		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("sub", u.getEmail(), "twoFactorAuthenticated", true, "exp",
				Instant.now().getEpochSecond() + 10 * 60);
		var t = Jwt.generateToken(h, p, configuration.getProperty("eshopweb.jwt.key"));
		exchange.addUserCookie(t);

		var b = ((CustomHttpExchange) exchange).getBasket(false);
		if (b != null) {
			transferBasket(b, u);
			exchange.removeBasketCookie();
		}

		return returnURI != null ? returnURI : URI.create("/");
	}

	@Handle(method = "GET", path = "/user/login/recovery")
	public Recovery getRecovery() {
		return new Recovery(null, null);
	}

	@Handle(method = "POST", path = "/user/login/recovery")
	public Object authenticate(Recovery.Form form, @Parameter(name = "returnUrl") URI returnURI,
			EShopWebApp.Exchange exchange) throws IOException {
		var v = new ValidationMessages();
		if (form.code.isBlank())
			v.set("code", "The Code field is required.");

		var u = v.isEmpty() ? exchange.getUser(false) : null;
		if (u != null) {
			var f = HexFormat.of();
			var s = f.parseHex(u.getSalt());
			var h = f.formatHex(ApplicationUser.hash(form.code.toCharArray(), s));
			if (u.getTwoFactor().recoveryCodeHashes().remove(h))
				persistence.getCrud(ApplicationUser.class)
						.update(u.getId(), x -> x.setTwoFactor(u.getTwoFactor()));
			else
				v.set(null, "Invalid login attempt.");
		}

		if (!v.isEmpty())
			return new Recovery(form, v);

		var h2 = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("sub", u.getEmail(), "twoFactorAuthenticated", true, "exp",
				Instant.now().getEpochSecond() + 10 * 60);
		var t = Jwt.generateToken(h2, p, configuration.getProperty("eshopweb.jwt.key"));
		exchange.addUserCookie(t);

		var b = ((CustomHttpExchange) exchange).getBasket(false);
		if (b != null) {
			transferBasket(b, u);
			exchange.removeBasketCookie();
		}

		return returnURI != null ? returnURI : URI.create("/");
	}

	@Handle(method = "POST", path = "/user/logout")
	public URI logout(EShopWebApp.Exchange exchange) {
		exchange.removeUserCookie();
		return URI.create("/");
	}

	@Handle(method = "GET", path = "/user/reset-password")
	public ResetPassword getResetPassword() {
		return new ResetPassword(null, null);
	}

	@Handle(method = "POST", path = "/user/reset-password")
	public Object resetPassword(ResetPassword.Form form, EShopWebApp.Exchange exchange) throws IOException {
		var v = new ValidationMessages();
		if (form.email.isBlank())
			v.set("email", "The Email field is required.");
		else if (!emailPattern.matcher(form.email).matches())
			v.set("email", "The Email field is not a valid e-mail address.");
		if (!v.isEmpty())
			return new ResetPassword(form, v);
		return URI.create("/user/reset-password-confirmation");
	}

	@Handle(method = "GET", path = "/user/reset-password-confirmation")
	public ResetPassword.Confirmation getResetPasswordConfirmation() {
		return new ResetPassword.Confirmation();
	}

	@Handle(method = "GET", path = "/user/register")
	public Register getRegister() {
		return new Register(null, null);
	}

	static Pattern nonAlphanumericPattern = Pattern.compile("[^A-Za-z0-9]");

	static Pattern digitPattern = Pattern.compile("[0-9]");

	static Pattern uppercasePattern = Pattern.compile("[A-Z]");

	@Handle(method = "POST", path = "/user/register")
	public Object createAccount(Register.Form form, EShopWebApp.Exchange exchange) throws IOException {
		var v = new ValidationMessages();
		if (form.email.isBlank())
			v.set("email", "The Email field is required.");
		else if (!emailPattern.matcher(form.email).matches())
			v.set("email", "The Email field is not a valid e-mail address.");
		if (form.password.isBlank())
			v.set("password", "The Password field is required.");
		else if (form.password.length() < 6 || form.password.length() > 100)
			v.set("password", "The Password must be at least 6 and at max 100 characters long.");
		else if (!form.confirmPassword.equals(form.password))
			v.set("confirmPassword", "The password and confirmation password do not match.");
		else {
			if (!nonAlphanumericPattern.matcher(form.password).find())
				v.add(null, "Passwords must have at least one non alphanumeric character.");
			if (!digitPattern.matcher(form.password).find())
				v.add(null, "Passwords must have at least one digit ('0'-'9').");
			if (!uppercasePattern.matcher(form.password).find())
				v.add(null, "Passwords must have at least one uppercase ('A'-'Z').");
		}
		var c = persistence.getCrud(ApplicationUser.class);
		if (v.isEmpty() && c.count("email", form.email) > 0)
			v.add(null, "Username '" + form.email + "' is already taken.");
		if (!v.isEmpty())
			return new Register(form, v);

		var u = new ApplicationUser();
		u.setUserName(form.email);
		u.setEmail(form.email);
		ApplicationUser.setHashAndSalt(u, form.password);
		c.create(u);

		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("sub", u.getEmail(), "twoFactorAuthenticated", false, "exp",
				Instant.now().getEpochSecond() + 10 * 60);
		var t = Jwt.generateToken(h, p, configuration.getProperty("eshopweb.jwt.key"));
		exchange.addUserCookie(t);

		var b = ((CustomHttpExchange) exchange).getBasket(false);
		if (b != null) {
			transferBasket(b, u);
			exchange.removeBasketCookie();
		}
		return URI.create("/");
	}

	protected void transferBasket(Basket anonymous, ApplicationUser user) throws IOException {
		var c = persistence.getCrud(BasketItem.class);
		var i = c.read(c.filter("basket", anonymous.getId())).toList();
		if (!i.isEmpty()) {
			var d = persistence.getCrud(Basket.class);
			var b = d.read(d.filter("buyer", user.getUserName())).findFirst().orElse(null);
			persistence.getDatabase().perform((ss, ii) -> {
				var b2 = b;
				if (b2 == null) {
					b2 = new Basket();
					b2.setBuyer(user.getUserName());
					d.create(b2);
				}
				for (var i1 : i) {
					var i2 = new BasketItem();
					i2.setBasket(b2.getId());
					Reflection.copy(i1, i2, n -> !Set.of("basket", "id").contains(n));
					c.create(i2);
				}
				return null;
			}, true);
		}
	}

	@Render(template = "Login.html")
	public record Login(Form form, ValidationMessages validationMessages) implements Page, ValidationView {

		@Override
		public String title() {
			return "Log in";
		}

		public record Form(String email, String password) {
		}
	}

	@Render(template = "Login-TwoFactor.html")
	public record TwoFactor(Form form, ValidationMessages validationMessages) implements Page, ValidationView {

		@Override
		public String title() {
			return "Two-factor authentication";
		}

		public record Form(String code) {
		}
	}

	@Render(template = "Login-Recovery.html")
	public record Recovery(Form form, ValidationMessages validationMessages) implements Page, ValidationView {

		@Override
		public String title() {
			return "Recovery code verification";
		}

		public record Form(String code) {
		}
	}

//	public record Current(boolean isAuthenticated, URI nameClaimType, URI roleClaimType, String token,
//			List<Claim> claims) {
//	}
//
//	public record Claim(URI type, String value) {
//	}

	@Render(template = "ResetPassword.html")
	public record ResetPassword(Form form, ValidationMessages validationMessages) implements Page, ValidationView {

		@Override
		public String title() {
			return "Forgot your password?";
		}

		public record Form(String email) {
		}

		@Render(template = "ResetPassword-Confirmation.html")
		public record Confirmation() implements Page {

			@Override
			public String title() {
				return "Forgot password confirmation";
			}
		}
	}

	@Render(template = "Register.html")
	public record Register(Form form, ValidationMessages validationMessages) implements Page, ValidationView {

		@Override
		public String title() {
			return "Register";
		}

		public record Form(String email, String password, String confirmPassword) {
		}
	}
}
