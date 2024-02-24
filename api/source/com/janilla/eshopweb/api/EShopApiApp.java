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
import java.util.function.Supplier;

import com.janilla.eshopweb.core.CustomApplicationPersistenceBuilder;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpServer;
import com.janilla.io.IO;
import com.janilla.persistence.Persistence;
import com.janilla.util.Lazy;
import com.janilla.web.AnnotationDrivenToMethodInvocation;

public class EShopApiApp {

	public static void main(String[] args) throws IOException {
		var a = new EShopApiApp();
		{
			var c = new Properties();
			try (var s = a.getClass().getResourceAsStream("configuration.properties")) {
				c.load(s);
			}
			a.setConfiguration(c);
		}
		a.getPersistence();

		var s = a.new Server();
		s.setPort(Integer.parseInt(a.getConfiguration().getProperty("eshopweb.api.server.port")));
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
		b.setApplication(this);
		return b.build();
	});

	AnnotationDrivenToMethodInvocation toInvocation;

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

	public AnnotationDrivenToMethodInvocation getToInvocation() {
		return toInvocation;
	}

	class Server extends HttpServer {

		@Override
		protected HttpExchange newExchange(HttpRequest request) {
			return new Exchange();
		}
	}

	public class Exchange extends CustomHttpExchange {
		{
			configuration = getConfiguration();
			persistence = getPersistence();
		}
	}
}
