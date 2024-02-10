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
import java.util.Map;
import java.util.Properties;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.http.HttpResponse;
import com.janilla.json.Jwt;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Parameter;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class LoginWeb {

	Properties configuration;

	private Persistence persistence;

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/Identity/Account/Login")
	public Object show() throws IOException {
		return new Foo();
	}

	@Handle(method = "POST", uri = "/Identity/Account/Login")
	public Object login(Bar bar, @Parameter(name = "returnUrl") URI returnURI, HttpResponse response)
			throws IOException {
		ApplicationUser u;
		{
			var c = persistence.getCrud(ApplicationUser.class);
			var i = c.find("email", bar.email);
			u = i >= 0 ? c.read(i) : null;
		}
		if (u != null && !ApplicationUser.testPassword(bar.password, u))
			u = null;
		if (u == null)
			throw new NullPointerException("u");
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.getEmail());
		var t = Jwt.generateToken(h, p, configuration.getProperty("eshopweb.web.jwt.key"));
		response.getHeaders().add("Set-Cookie", "EshopIdentifier=" + t + "; path=/; samesite=strict");
		return returnURI != null ? returnURI : URI.create("/");
	}

	@Render(template = "Login.html")
	public record Foo() {
	}

	public record Bar(@Parameter(name = "email") String email, @Parameter(name = "password") String password,
			@Parameter(name = "rememberMe") Boolean rememberMe) {
	}
}
