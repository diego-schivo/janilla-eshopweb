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
package com.janilla.eshopweb.full;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import com.janilla.eshopweb.admin.EShopAdminApp;
import com.janilla.eshopweb.api.EShopApiApp;
import com.janilla.eshopweb.web.EShopWebApp;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpServer;
import com.janilla.io.IO;
import com.janilla.util.Lazy;
import com.janilla.web.NotFoundException;

public class EShopFullApp {

	public static void main(String[] args) throws IOException {
		var a = new EShopFullApp();
		{
			var c = new Properties();
			try (var s = EShopFullApp.class.getResourceAsStream("configuration.properties")) {
				c.load(s);
			}
			a.setConfiguration(c);
		}

		var s = new HttpServer();
		s.setPort(Integer.parseInt(a.getConfiguration().getProperty("eshopweb.full.server.port")));
		s.setHandler(a.getHandler());
		s.run();
	}

	Properties configuration;

	Supplier<EShopApiApp> api = Lazy.of(() -> {
		var a = new EShopApiApp();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<EShopAdminApp> admin = Lazy.of(() -> {
		var a = new EShopAdminApp();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<EShopWebApp> web = Lazy.of(() -> {
		var a = new EShopWebApp();
		a.setConfiguration(configuration);
		return a;
	});

	static ThreadLocal<IO.Consumer<HttpExchange>> currentHandler = new ThreadLocal<>();

	Supplier<IO.Consumer<HttpExchange>> handler = Lazy.of(() -> {
		var hh = List.of(getAdmin().getHandler(), getWeb().getHandler(), getApi().getHandler());
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
				var f = e;
				if (h == hh.get(1))
					f = getWeb().new Exchange();
				if (h == hh.get(2))
					f = getApi().new Exchange();
				if (f != e) {
					f.setRequest(e.getRequest());
					f.setResponse(e.getResponse());
					f.setException(e.getException());
				}
				currentHandler.set(h);
				try {
					h.accept(f);
					currentHandler.remove();
					break;
				} catch (NotFoundException g) {
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

	public EShopApiApp getApi() {
		return api.get();
	}

	public EShopAdminApp getAdmin() {
		return admin.get();
	}

	public EShopWebApp getWeb() {
		return web.get();
	}

	public IO.Consumer<HttpExchange> getHandler() {
		return handler.get();
	}
}
