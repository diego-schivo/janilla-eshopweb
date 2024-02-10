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
import java.util.Collections;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.web.UserWeb.View.Claim;
import com.janilla.web.Handle;

public class UserWeb {

	static URI NAME_CLAIM_TYPE = URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name");

	static URI ROLE_CLAIM_TYPE = URI.create("http://schemas.microsoft.com/ws/2008/06/identity/claims/role");

	@Handle(method = "GET", uri = "/User")
	public Object show(ApplicationUser user) throws IOException {
		return new View(user != null, NAME_CLAIM_TYPE, ROLE_CLAIM_TYPE, null,
				user != null ? user.getRoles().stream().map(r -> new Claim(ROLE_CLAIM_TYPE, r)).toList()
						: Collections.emptyList());
	}

	public record View(boolean isAuthenticated, URI nameClaimType, URI roleClaimType, String token,
			Collection<Claim> claims) {

		public record Claim(URI type, String value) {
		}
	}
}
