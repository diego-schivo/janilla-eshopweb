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
package com.janilla.eshopweb.fullstack;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Supplier;

import com.janilla.eshopweb.admin.EShopOnWebAdmin;
import com.janilla.eshopweb.api.EShopOnWebApi;
import com.janilla.eshopweb.web.EShopOnWebWeb;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpServer;
import com.janilla.io.IO;
import com.janilla.util.Lazy;
import com.janilla.web.NotFoundException;

public class EShopOnWebFullstack {

	public static void main(String[] args) throws IOException {
		var c = new Properties();
		try (var s = EShopOnWebFullstack.class.getResourceAsStream("configuration.properties")) {
			c.load(s);
		}

		var f = new EShopOnWebFullstack();
		f.setConfiguration(c);

		var s = new HttpServer();
		s.setExecutor(Runnable::run);
		s.setPort(Integer.parseInt(c.getProperty("eshopweb.fullstack.http.port")));
		s.setHandler(f.getHandler());
		s.run();
	}

	Properties configuration;

	Supplier<EShopOnWebWeb> web = Lazy.of(() -> {
		var w = new EShopOnWebWeb();
		w.setConfiguration(configuration);
		return w;
	});

	Supplier<EShopOnWebAdmin> admin = Lazy.of(() -> {
		var a = new EShopOnWebAdmin();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<EShopOnWebApi> api = Lazy.of(() -> {
		var a = new EShopOnWebApi();
		a.setConfiguration(configuration);
		return a;
	});

	Supplier<IO.Consumer<HttpExchange>> handler = Lazy.of(() -> {
		var h1 = getWeb().getHandler();
		var h2 = getAdmin().getHandler();
		var h3 = getApi().getHandler();
		return c -> {
//			var o = c.getException() != null ? c.getException() : c.getRequest();
//			var h = switch (o) {
//			case HttpRequest q -> {
//				URI u;
//				try {
//					u = q.getURI();
//				} catch (NullPointerException e) {
//					u = null;
//				}
//				var p = u != null ? u.getPath() : null;
//				if (p != null && (p.equals("/Admin") || p.startsWith("/admin.")))
//					yield admin.get().getHandler();
//				if (p != null && p.startsWith("/api/"))
//					yield api.get().getHandler();
//				yield web.get().getHandler();
//			}
//			case Exception e -> api.get().getHandler();
//			default -> null;
//			};
//			h.accept(c);
			try {
				h2.accept(c);
			} catch (NotFoundException e) {
				try {
					var d = getWeb().newExchange();
					d.setRequest(c.getRequest());
					d.setResponse(c.getResponse());
					h1.accept(d);
				} catch (NotFoundException f) {
					h3.accept(c);
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

	public EShopOnWebWeb getWeb() {
		return web.get();
	}

	public EShopOnWebAdmin getAdmin() {
		return admin.get();
	}

	public EShopOnWebApi getApi() {
		return api.get();
	}

	public IO.Consumer<HttpExchange> getHandler() {
		return handler.get();
	}
}
