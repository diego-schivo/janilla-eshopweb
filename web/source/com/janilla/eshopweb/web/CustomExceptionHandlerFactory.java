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

import com.janilla.frontend.RenderEngine.Entry;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpResponse.Status;
import com.janilla.net.Net;
import com.janilla.util.EntryList;
import com.janilla.web.Error;
import com.janilla.web.ExceptionHandlerFactory;
import com.janilla.web.HandlerFactory;
import com.janilla.web.UnauthenticatedException;

public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

	protected HandlerFactory mainFactory;

	public void setMainFactory(HandlerFactory mainFactory) {
		this.mainFactory = mainFactory;
	}

	@Override
	protected void handle(Error error, HttpExchange exchange) throws IOException {
		super.handle(error, exchange);

		var e = exchange.getException();
		{
			var p = switch (e) {
			case UnauthenticatedException f -> "/user/login";
			case TwoFactorAuthenticationException f -> "/user/login/two-factor";
			default -> null;
			};
			if (p != null) {
				var q = new EntryList<String, String>();
				q.add("returnUrl", exchange.getRequest().getURI().toString());
				var u = URI.create("/user/login?" + Net.formatQueryString(q));
				var s = exchange.getResponse();
				s.setStatus(new Status(302, "Found"));
				s.getHeaders().set("Cache-Control", "no-cache");
				s.getHeaders().set("Location", u.toString());
				return;
			}
		}

		if (e instanceof Page p) {
			var l = CustomTemplateHandlerFactory.toLayout(p, exchange);
			var h = mainFactory.createHandler(new Entry(null, l, null), exchange);
			h.accept(exchange);
		}
	}
}
