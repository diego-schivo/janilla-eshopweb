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
import java.util.Properties;
import java.util.function.Supplier;

import com.janilla.eshopweb.core.CustomApplicationPersistenceBuilder;
import com.janilla.http.HttpExchange;
import com.janilla.io.IO;
import com.janilla.persistence.Persistence;
import com.janilla.util.Lazy;

public class EShopOnWebWeb {

	public static void main(String[] args) throws IOException {
		var p = new Properties();
		try (var s = EShopOnWebWeb.class.getResourceAsStream("configuration.properties")) {
			p.load(s);
		}

		var a = new EShopOnWebWeb();
		a.setConfiguration(p);
		a.getPersistence();

		var s = new CustomHttpServer();
		s.eShopOnWeb = a;
		s.setPort(Integer.parseInt(p.getProperty("eshopweb.web.http.port")));
		s.setHandler(a.getHandler());
		s.run();
	}

	private Properties configuration;

	private IO.Supplier<Persistence> persistence = IO.Lazy.of(() -> {
		var b = new CustomApplicationPersistenceBuilder();
		b.setApplication(this);
		return b.build();
	});

	Supplier<IO.Consumer<HttpExchange>> handler = Lazy.of(() -> {
		var b = new CustomApplicationHandlerBuilder();
		b.setApplication(EShopOnWebWeb.this);
		return b.build();
	});

	public Properties getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public Persistence getPersistence() {
		try {
			return persistence.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public IO.Consumer<HttpExchange> getHandler() {
		return handler.get();
	}

	public HttpExchange newExchange() {
		var c = new CustomHttpExchange();
		c.configuration = getConfiguration();
		c.persistence = getPersistence();
		return c;
	}
}
