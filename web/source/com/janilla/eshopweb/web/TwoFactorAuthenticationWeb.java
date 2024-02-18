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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.web.AccountWeb.Account;
import com.janilla.http.HttpExchange;
import com.janilla.json.Jwt;
import com.janilla.net.Net;
import com.janilla.persistence.Persistence;
import com.janilla.util.Base32;
import com.janilla.util.EntryList;
import com.janilla.util.Totp;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class TwoFactorAuthenticationWeb {

	Properties configuration;

	private Persistence persistence;

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/account/authenticator")
	public Account getAuthenticator(HttpExchange exchange) throws IOException {
		var u = ((CustomHttpExchange) exchange).getUser(true);
		return new Account(new Authenticator(u));
	}

	@Handle(method = "GET", uri = "/account/authenticator/enable")
	public Account getEnable(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().secretKey() == null)
			setSecretKey(u);
		var k = u.getTwoFactor().secretKey();
		var f = formatSecretKey(k);
		var q = getQRCode(u.getEmail(), k);
		return new Account(new Enable(f, q));
	}

	@Handle(method = "POST", uri = "/account/authenticator/enable")
	public Object enable(Enable.Form form, HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		var k = Base32.decode(u.getTwoFactor().secretKey());
		var c = Totp.getCode(k);
//		System.out.println("c " + c);
		if (!form.code.equals(c))
			return getEnable(exchange);

		u.setTwoFactor(u.getTwoFactor().withEnabled(true));
		persistence.getCrud(ApplicationUser.class).update(u.getId(), x -> x.setTwoFactor(u.getTwoFactor()));

		var t = getJwt(u.getEmail(), true);
		e.addUserCookie(t);

		return URI.create("/account/authenticator/recovery");
	}

	@Handle(method = "GET", uri = "/account/authenticator/disable")
	public Account getDisable(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (!u.getTwoFactor().enabled())
			throw new RuntimeException();
		return new Account(new Disable());
	}

	@Handle(method = "POST", uri = "/account/authenticator/disable")
	public Object disable(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (!u.getTwoFactor().enabled())
			throw new RuntimeException();
		u.setTwoFactor(new ApplicationUser.TwoFactor(false, u.getTwoFactor().secretKey(), null));
		persistence.getCrud(ApplicationUser.class).update(u.getId(), x -> x.setTwoFactor(u.getTwoFactor()));
		return URI.create("/account/authenticator");
	}

	@Handle(method = "GET", uri = "/account/authenticator/recovery")
	public Account getRecovery(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().recoveryCodeHashes() != null)
			throw new RuntimeException();
		var c = setRecoveryCodes(u);
		return new Account(new Recovery(c));
	}

	@Handle(method = "GET", uri = "/account/authenticator/reset")
	public Account getReset(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().secretKey() == null)
			throw new RuntimeException();
		return new Account(new Reset());
	}

	@Handle(method = "POST", uri = "/account/authenticator/reset")
	public Object reset(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().secretKey() == null)
			throw new RuntimeException();
		u.setTwoFactor(new ApplicationUser.TwoFactor(false, null, null));
		persistence.getCrud(ApplicationUser.class).update(u.getId(), x -> x.setTwoFactor(u.getTwoFactor()));
		return URI.create("/account/authenticator/enable");
	}

	@Handle(method = "GET", uri = "/account/authenticator/recovery/reset")
	public Account getRecoveryReset(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().recoveryCodeHashes() == null)
			throw new RuntimeException();
		return new Account(new RecoveryReset());
	}

	@Handle(method = "POST", uri = "/account/authenticator/recovery/reset")
	public Object resetRecovery(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().recoveryCodeHashes() == null)
			throw new RuntimeException();
		u.setTwoFactor(u.getTwoFactor().withRecoveryCodeHashes(null));
		persistence.getCrud(ApplicationUser.class).update(u.getId(), x -> x.setTwoFactor(u.getTwoFactor()));
		return URI.create("/account/authenticator/recovery");
	}

	protected void setSecretKey(ApplicationUser user) throws IOException {
		var b = new byte[20];
		ApplicationUser.RANDOM.nextBytes(b);
		user.setTwoFactor(user.getTwoFactor().withSecretKey(Base32.encode(b)));
		persistence.getCrud(ApplicationUser.class).update(user.getId(), x -> x.setTwoFactor(user.getTwoFactor()));
	}

	protected List<String> setRecoveryCodes(ApplicationUser user) throws IOException {
		var c = new ArrayList<String>();
		for (var i = 0; i < 10; i++) {
			var s = IntStream.range(0, 10).map(x -> ApplicationUser.RANDOM.nextInt(0, 26 + 10))
					.map(x -> x < 26 ? 'A' + x : '0' + x - 26)
					.collect(StringBuilder::new, (b, x) -> b.append((char) x), (b1, b2) -> {
					}).toString();
			c.add(s.substring(0, 5) + "-" + s.substring(5));
		}
		var f = HexFormat.of();
		var s = f.parseHex(user.getSalt());
		var h = c.stream().map(x -> f.formatHex(ApplicationUser.hash(x.toCharArray(), s))).collect(Collectors.toSet());
		user.setTwoFactor(user.getTwoFactor().withRecoveryCodeHashes(h));
		persistence.getCrud(ApplicationUser.class).update(user.getId(), x -> x.setTwoFactor(user.getTwoFactor()));
		return c;
	}

	protected String formatSecretKey(String key) {
		return IntStream.range(0, Math.ceilDiv(key.length(), 4))
				.mapToObj(i -> key.substring(i * 4, Math.min((i + 1) * 4, key.length())).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	protected URI getQRCode(String email, String secret) {
		var q = new EntryList<String, String>();
		q.add("secret", secret);
		q.add("issuer", "eShopOnWeb");
		q.add("digits", "6");
		return URI.create("otpauth://totp/eShopOnWeb:" + email + "?" + Net.formatQueryString(q));
	}

	protected String getJwt(String email, boolean twoFactor) {
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("sub", email, "twoFactorAuthenticated", twoFactor); // "exp", Instant.now().getEpochSecond() + 7
																			// * 24 * 60 * 60
		return Jwt.generateToken(h, p, configuration.getProperty("eshopweb.jwt.key"));
	}

	@Render(template = "Authenticator.html")
	public record Authenticator(ApplicationUser user) implements Page {

		@Override
		public String title() {
			return "Two-factor authentication";
		}

		public Manage manage() {
			return user.getTwoFactor().enabled() ? new Manage() : null;
		}

		public AppCreate appCreate() {
			return user.getTwoFactor().secretKey() == null ? new AppCreate() : null;
		}

		public AppUpdate appUpdate() {
			return user.getTwoFactor().secretKey() != null ? new AppUpdate() : null;
		}

		@Render(template = "Authenticator-Manage.html")
		public record Manage() {
		}

		@Render(template = "Authenticator-AppCreate.html")
		public record AppCreate() {
		}

		@Render(template = "Authenticator-AppUpdate.html")
		public record AppUpdate() {
		}
	}

	@Render(template = "Authenticator-Enable.html")
	public record Enable(String sharedKey, URI qrCode) implements Page {

		@Override
		public String title() {
			return "Enable authenticator";
		}

		public record Form(String code) {
		}
	}

	@Render(template = "Authenticator-Disable.html")
	public record Disable() implements Page {

		@Override
		public String title() {
			return "Disable two-factor authentication (2FA)";
		}
	}

	@Render(template = "Authenticator-Recovery.html")
	public record Recovery(List<String> codes) implements Page {

		@Override
		public String title() {
			return "Recovery codes";
		}

		public Stream<Code> codeStream() {
			var i = new int[1];
			return codes.stream().map(x -> new Code(i[0]++, x));
		}

		@Render(template = """
				<code>${value}</code>${delimiter}
				""")
		public record Code(int index, String value) {

			public String delimiter() {
				return index % 2 == 0 ? " " : "<br />";
			}
		}
	}

	@Render(template = "Authenticator-Reset.html")
	public record Reset() implements Page {

		@Override
		public String title() {
			return "Reset authenticator key";
		}
	}

	@Render(template = "Authenticator-RecoveryReset.html")
	public record RecoveryReset() implements Page {

		@Override
		public String title() {
			return "Generate two-factor authentication (2FA) recovery codes";
		}
	}
}
