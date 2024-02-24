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
package com.janilla.eshopweb.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.http.HttpExchange;
import com.janilla.io.IO;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.web.ForbiddenException;
import com.janilla.web.UnauthenticatedException;

abstract class CustomHttpExchange extends HttpExchange {

	Properties configuration;

	Persistence persistence;

	private IO.Supplier<ApplicationUser> user = IO.Lazy.of(() -> {
		var a = getRequest().getHeaders().get("Authorization");
		var t = a != null && a.startsWith("Bearer ") ? a.substring("Bearer ".length()) : null;
		var p = t != null ? Jwt.verifyToken(t, configuration.getProperty("eshopweb.jwt.key")) : null;
		var e = p != null ? (String) p.get("sub") : null;
		var c = persistence.getCrud(ApplicationUser.class);
		var i = e != null ? c.find("email", e) : -1;
		var u = i >= 0 ? c.read(i) : null;
		return u;
	});

	public ApplicationUser getUser() {
		try {
			return user.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void requireAdministrator() {
		var u = getUser();
		if (u == null)
			throw new UnauthenticatedException();
		if (!u.getRoles().contains("Administrators"))
			throw new ForbiddenException();
	}
}
