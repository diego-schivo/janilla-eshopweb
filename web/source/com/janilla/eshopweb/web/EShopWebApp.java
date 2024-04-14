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
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import com.janilla.eshopweb.admin.EShopAdminApp;
import com.janilla.eshopweb.core.CustomPersistenceBuilder;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpServer;
import com.janilla.io.IO;
import com.janilla.persistence.Persistence;
import com.janilla.util.Lazy;
import com.janilla.web.NotFoundException;

public class EShopWebApp {

	public static void main(String[] args) throws IOException {
		var p = new Properties();
		try (var s = EShopWebApp.class.getResourceAsStream("configuration.properties")) {
			p.load(s);
		}

		var a = new EShopWebApp();
		a.setConfiguration(p);
		a.getPersistence();

		var s = new HttpServer();
		s.setPort(Integer.parseInt(p.getProperty("eshopweb.web.server.port")));
		s.setHandler(a.getHandler());
		s.run();
	}

	private Properties configuration;

	private IO.Supplier<Persistence> persistence = IO.Lazy.of(() -> {
		var b = new CustomPersistenceBuilder();
		b.setApplication(this);
		return b.build();
	});

	Supplier<EShopAdminApp> admin = Lazy.of(() -> {
		var a = new EShopAdminApp();
		a.setConfiguration(configuration);
		return a;
	});

	static ThreadLocal<IO.Consumer<HttpExchange>> currentHandler = new ThreadLocal<>();

	Supplier<IO.Consumer<HttpExchange>> handler = Lazy.of(() -> {
		var b = new CustomApplicationHandlerBuilder();
		b.setApplication(this);
		var hh = List.of(getAdmin().getHandler(), b.build());
		return e -> {
			try {
				e.getRequest().getURI();
//				System.out.println("u " + u);
			} catch (NullPointerException f) {
				f.printStackTrace();
				return;
			}
			var h = currentHandler.get();
			var n = h == null;
			if (n)
				h = hh.get(0);
			for (;;) {
				if (h == hh.get(1)) {
					var f = new Exchange();
					f.setRequest(e.getRequest());
					f.setResponse(e.getResponse());
					f.setException(e.getException());
					e = f;
				}
				currentHandler.set(h);
				try {
					h.accept(e);
					currentHandler.remove();
					break;
				} catch (NotFoundException f) {
					var i = n ? hh.indexOf(h) + 1 : -1;
					if (i < 0 || i >= hh.size())
						throw new NotFoundException();
					h = hh.get(i);
				}
			}
		};
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

	public EShopAdminApp getAdmin() {
		return admin.get();
	}

	public IO.Consumer<HttpExchange> getHandler() {
		return handler.get();
	}

	public class Exchange extends CustomHttpExchange {
		{
			configuration = getConfiguration();
			persistence = getPersistence();
		}
	}
}
