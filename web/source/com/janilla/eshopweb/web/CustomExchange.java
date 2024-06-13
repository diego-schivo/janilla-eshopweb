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
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.core.Basket;
import com.janilla.http.Http;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHeader;
import com.janilla.io.IO;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.web.UnauthenticatedException;

public class CustomExchange extends HttpExchange {

	public Properties configuration;

	public Persistence persistence;

	private boolean authenticateUser;

	private IO.Supplier<ApplicationUser> user = IO.Lazy.of(() -> {
		var c = persistence.crud(ApplicationUser.class);
		Map<String, ?> p;
		{
			var t = getUserCookie();
//			System.out.println("t=" + t);
			try {
				p = t != null ? Jwt.verifyToken(t, configuration.getProperty("eshopweb.jwt.key")) : null;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				p = null;
			}
		}
		var e = p != null /* && (Long) p.get("exp") < Instant.now().getEpochSecond() */ ? (String) p.get("sub") : null;
		var i = e != null ? c.find("email", e) : -1;
		var u = i > 0 ? c.read(i) : null;
		if (authenticateUser) {
			if (u == null)
				throw new UnauthenticatedException();
			if (u.getTwoFactor().enabled() && !((Boolean) p.get("twoFactorAuthenticated")))
				throw new TwoFactorAuthenticationException();
		}
		return u;
	});

	private boolean createBasket;

	private IO.Supplier<Basket> basket = IO.Lazy.of(() -> {
		var u = getUser(false);
		var i = u != null ? u.getUserName() : null;
		if (i == null)
			i = getBasketCookie();
		var c = persistence.crud(Basket.class);
		var b = i != null ? c.read(c.find("buyer", i)) : null;
		if (b == null && createBasket) {

			if (Boolean.parseBoolean(configuration.getProperty("eshopweb.live-demo"))
					&& persistence.crud(Basket.class).count() >= 1000)
				throw new MethodBlockedException();

			var d = new Basket();
			d.setBuyer(u != null ? u.getUserName() : UUID.randomUUID().toString());
			c.create(d);
			b = d;
			if (u == null)
				addBasketCookie(b.getBuyer());
		}
		return b;
	});

	public ApplicationUser getUser(boolean authenticate) {
		authenticateUser = authenticate;
		try {
			return user.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Basket getBasket(boolean create) {
		createBasket = create;
		try {
			return basket.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static String BASKET_COOKIE = "basket";

	public void addBasketCookie(String value) {
		getResponse().getHeaders().add(new HttpHeader("Set-Cookie", Http.formatSetCookieHeader(BASKET_COOKIE, value,
				ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).plusYears(10), "/", "strict")));
	}

	public String getBasketCookie() {
		var c = getRequest().getHeaders().stream().filter(x -> x.name().equals("Cookie")).map(HttpHeader::value)
				.findFirst().orElse(null);
		return c != null ? Http.parseCookieHeader(c).get(BASKET_COOKIE) : null;
	}

	public void removeBasketCookie() {
		getResponse().getHeaders().add(new HttpHeader("Set-Cookie", Http.formatSetCookieHeader(BASKET_COOKIE, null,
				ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "/", "strict")));
	}

	static String USER_COOKIE = "user";

	public void addUserCookie(String value) {
		getResponse().getHeaders()
				.add(new HttpHeader("Set-Cookie", Http.formatSetCookieHeader(USER_COOKIE, value, null, "/", "strict")));
	}

	public String getUserCookie() {
		var c = getRequest().getHeaders().stream().filter(x -> x.name().equals("Cookie")).map(HttpHeader::value)
				.findFirst().orElse(null);
		return c != null ? Http.parseCookieHeader(c).get(USER_COOKIE) : null;
	}

	public void removeUserCookie() {
		getResponse().getHeaders().add(new HttpHeader("Set-Cookie", Http.formatSetCookieHeader(USER_COOKIE, null,
				ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "/", "strict")));
	}
}
