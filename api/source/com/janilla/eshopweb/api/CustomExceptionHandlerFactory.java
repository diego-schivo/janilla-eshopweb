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

import com.janilla.frontend.RenderEngine;
import com.janilla.http.HttpExchange;
import com.janilla.web.Error;
import com.janilla.web.ExceptionHandlerFactory;
import com.janilla.web.WebHandlerFactory;

public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

	protected WebHandlerFactory mainFactory;

	public void setMainFactory(WebHandlerFactory mainFactory) {
		this.mainFactory = mainFactory;
	}

	@Override
	protected boolean handle(Error error, HttpExchange exchange) {
		super.handle(error, exchange);
		var e = exchange.getException();
		if (e instanceof Exception)
			mainFactory.createHandler(RenderEngine.Entry.of(null, e.getMessage(), null), exchange).handle(exchange);
		return true;
	}
}
