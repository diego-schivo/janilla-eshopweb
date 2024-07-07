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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import com.janilla.eshopweb.admin.EShopAdminApp;
import com.janilla.eshopweb.api.EShopApiApp;
import com.janilla.eshopweb.web.EShopWebApp;
import com.janilla.http.HttpExchange;
import com.janilla.net.Server;
import com.janilla.util.Lazy;
import com.janilla.web.NotFoundException;

public class EShopFullApp {

	public static void main(String[] args) throws Exception {
		var a = new EShopFullApp();
		{
			var c = new Properties();
			try (var s = EShopFullApp.class.getResourceAsStream("configuration.properties")) {
				c.load(s);
			}
			a.setConfiguration(c);
		}
		a.getApi().getPersistence();

		var s = new Server();
		s.setAddress(
				new InetSocketAddress(Integer.parseInt(a.getConfiguration().getProperty("eshopweb.full.server.port"))));
		s.setHandler(a.getHandler());
		s.serve();
	}

	public Properties configuration;

	Supplier<EShopApiApp> api = Lazy.of(() -> {
		var a = new EShopApiApp();
		a.configuration = configuration;
		return a;
	});

	Supplier<EShopAdminApp> admin = Lazy.of(() -> {
		var a = new EShopAdminApp();
		a.configuration = configuration;
		return a;
	});

	Supplier<EShopWebApp> web = Lazy.of(() -> {
		var a = new EShopWebApp();
		a.configuration = configuration;
		return a;
	});

	static ThreadLocal<Server.Handler> currentHandler = new ThreadLocal<>();

	Supplier<Server.Handler> handler = Lazy.of(() -> {
		var hh = List.of(getAdmin().getHandler(), getWeb().getHandler(), getApi().getHandler());
		return x -> {
			var e = (HttpExchange) x;
			try {
				e.getRequest().getUri();
//				System.out.println("u " + u);
			} catch (NullPointerException f) {
				f.printStackTrace();
				return false;
			}
			var h = currentHandler.get();
			var n = h == null;
			if (n)
				h = hh.get(0);
			for (;;) {
				var f = e;
				if (h == hh.get(1))
					f = getWeb().getFactory().create(HttpExchange.class);
				if (h == hh.get(2))
					f = getApi().getFactory().create(HttpExchange.class);
				if (f != e) {
					f.setRequest(e.getRequest());
					f.setResponse(e.getResponse());
					// TODO
					// f.setException(e.getException());
				}
				currentHandler.set(h);
				try {
					return h.handle(f);
				} catch (NotFoundException g) {
					var i = n ? hh.indexOf(h) + 1 : -1;
					if (i < 0 || i >= hh.size())
						throw new NotFoundException();
					h = hh.get(i);
				} finally {
					currentHandler.remove();
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

	public Server.Handler getHandler() {
		return handler.get();
	}
}
