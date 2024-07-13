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

	@Handle(method = "GET", path = "/account/authenticator")
	public AccountWeb.Account getAuthenticator(HttpExchange exchange) throws IOException {
		var u = ((CustomExchange) exchange).getUser(true);
		return new AccountWeb.Account(new Authenticator(u));
	}

	@Handle(method = "GET", path = "/account/authenticator/enable")
	public AccountWeb.Account getEnable(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().secretKey() == null)
			setSecretKey(u);
		var k = u.getTwoFactor().secretKey();
		var f = formatSecretKey(k);
		var q = getQRCode(u.getEmail(), k);
		return new AccountWeb.Account(new Enable(f, q));
	}

	@Handle(method = "POST", path = "/account/authenticator/enable")
	public Object enable(Enable.Form form, HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		var k = Base32.decode(u.getTwoFactor().secretKey());
		var c = Totp.getCode(k);
//		System.out.println("c " + c);
		if (!form.code.equals(c))
			return getEnable(exchange);

		u.setTwoFactor(u.getTwoFactor().withEnabled(true));
		persistence.crud(ApplicationUser.class).update(u.getId(), x -> {
			x.setTwoFactor(u.getTwoFactor());
			return x;
		});

		var t = getJwt(u.getEmail(), true);
		e.addUserCookie(t);

		return URI.create("/account/authenticator/recovery");
	}

	@Handle(method = "GET", path = "/account/authenticator/disable")
	public AccountWeb.Account getDisable(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (!u.getTwoFactor().enabled())
			throw new RuntimeException();
		return new AccountWeb.Account(new Disable());
	}

	@Handle(method = "POST", path = "/account/authenticator/disable")
	public Object disable(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (!u.getTwoFactor().enabled())
			throw new RuntimeException();
		u.setTwoFactor(new ApplicationUser.TwoFactor(false, u.getTwoFactor().secretKey(), null));
		persistence.crud(ApplicationUser.class).update(u.getId(), x -> {
			x.setTwoFactor(u.getTwoFactor());
			return x;
		});
		return URI.create("/account/authenticator");
	}

	@Handle(method = "GET", path = "/account/authenticator/recovery")
	public AccountWeb.Account getRecovery(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().recoveryCodeHashes() != null)
			throw new RuntimeException();
		var c = setRecoveryCodes(u);
		return new AccountWeb.Account(new Recovery(c));
	}

	@Handle(method = "GET", path = "/account/authenticator/reset")
	public AccountWeb.Account getReset(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().secretKey() == null)
			throw new RuntimeException();
		return new AccountWeb.Account(new Reset());
	}

	@Handle(method = "POST", path = "/account/authenticator/reset")
	public Object reset(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().secretKey() == null)
			throw new RuntimeException();
		u.setTwoFactor(new ApplicationUser.TwoFactor(false, null, null));
		persistence.crud(ApplicationUser.class).update(u.getId(), x -> {
			x.setTwoFactor(u.getTwoFactor());
			return x;
		});
		return URI.create("/account/authenticator/enable");
	}

	@Handle(method = "GET", path = "/account/authenticator/recovery/reset")
	public AccountWeb.Account getRecoveryReset(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().recoveryCodeHashes() == null)
			throw new RuntimeException();
		return new AccountWeb.Account(new RecoveryReset());
	}

	@Handle(method = "POST", path = "/account/authenticator/recovery/reset")
	public Object resetRecovery(HttpExchange exchange) throws IOException {
		var e = (CustomExchange) exchange;
		var u = e.getUser(true);
		if (u.getTwoFactor().recoveryCodeHashes() == null)
			throw new RuntimeException();
		u.setTwoFactor(u.getTwoFactor().withRecoveryCodeHashes(null));
		persistence.crud(ApplicationUser.class).update(u.getId(), x -> {
			x.setTwoFactor(u.getTwoFactor());
			return x;
		});
		return URI.create("/account/authenticator/recovery");
	}

	protected void setSecretKey(ApplicationUser user) throws IOException {
		var b = new byte[20];
		ApplicationUser.RANDOM.nextBytes(b);
		user.setTwoFactor(user.getTwoFactor().withSecretKey(Base32.encode(b)));
		persistence.crud(ApplicationUser.class).update(user.getId(), x -> {
			x.setTwoFactor(user.getTwoFactor());
			return x;
		});
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
		persistence.crud(ApplicationUser.class).update(user.getId(), x -> {
			x.setTwoFactor(user.getTwoFactor());
			return x;
		});
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

	@Render("Authenticator.html")
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

		@Render("Authenticator-Manage.html")
		public record Manage() {
		}

		@Render("Authenticator-AppCreate.html")
		public record AppCreate() {
		}

		@Render("Authenticator-AppUpdate.html")
		public record AppUpdate() {
		}
	}

	@Render("Authenticator-Enable.html")
	public record Enable(String sharedKey, URI qrCode) implements Page {

		@Override
		public String title() {
			return "Enable authenticator";
		}

		public record Form(String code) {
		}
	}

	@Render("Authenticator-Disable.html")
	public record Disable() implements Page {

		@Override
		public String title() {
			return "Disable two-factor authentication (2FA)";
		}
	}

	@Render("Authenticator-Recovery.html")
	public record Recovery(List<String> codes) implements Page {

		@Override
		public String title() {
			return "Recovery codes";
		}

		public Stream<Code> codeStream() {
			var i = new int[1];
			return codes.stream().map(x -> new Code(i[0]++, x));
		}

		@Render("""
				<code>{value}</code>{delimiter}
				""")
		public record Code(int index, String value) {

			public String delimiter() {
				return index % 2 == 0 ? " " : "<br />";
			}
		}
	}

	@Render("Authenticator-Reset.html")
	public record Reset() implements Page {

		@Override
		public String title() {
			return "Reset authenticator key";
		}
	}

	@Render("Authenticator-RecoveryReset.html")
	public record RecoveryReset() implements Page {

		@Override
		public String title() {
			return "Generate two-factor authentication (2FA) recovery codes";
		}
	}
}
