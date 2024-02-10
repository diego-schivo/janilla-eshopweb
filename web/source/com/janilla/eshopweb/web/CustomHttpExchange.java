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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.core.Basket;
import com.janilla.http.HttpExchange;
import com.janilla.io.IO;
import com.janilla.json.Jwt;
import com.janilla.net.Net;
import com.janilla.persistence.Persistence;

class CustomHttpExchange extends HttpExchange {

	Properties configuration;

	Persistence persistence;

	private IO.Supplier<ApplicationUser> user = IO.Lazy.of(() -> {
		Map<String, ?> p;
		{
			var c = getRequest().getHeaders().get("Cookie");
			var t = c != null ? Net.parseCookieHeader(c).get("EshopIdentifier") : null;
			try {
				p = t != null ? Jwt.verifyToken(t, configuration.getProperty("eshopweb.web.jwt.key")) : null;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				p = null;
			}
		}
		var e = p != null ? (String) p.get("loggedInAs") : null;
		var c = persistence.getCrud(ApplicationUser.class);
		var i = e != null ? c.find("email", e) : -1;
		var u = i >= 0 ? c.read(i) : null;
		return u;
	});

	private boolean createBasket;

	private IO.Supplier<Basket> basket = IO.Lazy.of(() -> {
		var u = getUser();
		var i = u != null ? u.getUserName() : null;
		if (i == null) {
			var c = getRequest().getHeaders().get("Cookie");
			i = c != null ? Net.parseCookieHeader(c).get("eShop") : null;
		}
		var c = persistence.getCrud(Basket.class);
		var b = i != null ? c.read(c.find("buyer", i)) : null;
		if (b == null && createBasket) {
			var d = new Basket();
			d.setBuyer(u != null ? u.getUserName() : UUID.randomUUID().toString());
			persistence.getDatabase().performTransaction(() -> c.create(d));
			b = d;
			if (u == null)
				getResponse()
						.getHeaders().add(
								"Set-Cookie", "eShop="
										+ b.getBuyer() + "; expires=" + ZonedDateTime.now(ZoneOffset.UTC)
												.truncatedTo(ChronoUnit.DAYS).plusYears(10).format(DateTimeFormatter
														.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH))
										+ "; path=/; samesite=strict");
		}
		return b;
	});

	public ApplicationUser getUser() {
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
}
