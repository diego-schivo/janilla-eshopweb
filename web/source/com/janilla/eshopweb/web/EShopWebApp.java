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
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.eshopweb.admin.EShopAdminApp;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.net.Net;
import com.janilla.net.Server;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Factory;
import com.janilla.util.Util;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.NotFoundException;

public class EShopWebApp {

	public static void main(String[] args) throws Exception {
		var pp = new Properties();
		try (var is = EShopWebApp.class.getResourceAsStream("configuration.properties")) {
			pp.load(is);
			if (args.length > 0) {
				var p = args[0];
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				pp.load(Files.newInputStream(Path.of(p)));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		var a = new EShopWebApp(pp);

		var hp = a.factory.create(HttpProtocol.class);
		try (var is = Net.class.getResourceAsStream("testkeys")) {
			hp.setSslContext(Net.getSSLContext("JKS", is, "passphrase".toCharArray()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		hp.setHandler(a.handler);

		var s = new Server();
		s.setAddress(new InetSocketAddress(Integer.parseInt(a.configuration.getProperty("eshopweb.web.server.port"))));
		s.setProtocol(hp);
		s.serve();
	}

	public Properties configuration;

	public Factory factory;

	public Persistence persistence;

	public EShopAdminApp admin;

	public HttpHandler handler;

	static ThreadLocal<HttpHandler> currentHandler = new ThreadLocal<>();

	public EShopWebApp(Properties configuration) {
		this.configuration = configuration;

		factory = new Factory();
		factory.setTypes(Stream.concat(Util.getPackageClasses("com.janilla.eshopweb.core"),
				Util.getPackageClasses(getClass().getPackageName())).toList());
		factory.setSource(this);

		admin = new EShopAdminApp(configuration);

		{
			var b = factory.create(ApplicationHandlerBuilder.class);
			var hh = List.of(admin.handler, b.build());
			handler = x -> {
				var he = (HttpExchange) x;
				try {
					he.getRequest().getPath();
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
					if (h == hh.get(1)) {
						var f = factory.create(HttpExchange.class);
						f.setRequest(he.getRequest());
						f.setResponse(he.getResponse());
						f.setException(he.getException());
						he = f;
					}
					currentHandler.set(h);
					try {
						return h.handle(he);
					} catch (NotFoundException f) {
						var i = n ? hh.indexOf(h) + 1 : -1;
						if (i < 0 || i >= hh.size())
							throw new NotFoundException();
						h = hh.get(i);
					} finally {
						currentHandler.remove();
					}
				}
			};
		}

		{
			var pb = factory.create(ApplicationPersistenceBuilder.class);
			var p = configuration.getProperty("eshopweb.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			pb.setFile(Path.of(p));
			persistence = pb.build();
		}
	}

	public EShopWebApp getApplication() {
		return this;
	}
}
