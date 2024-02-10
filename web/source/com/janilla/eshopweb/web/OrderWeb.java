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
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collection;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.core.Order;
import com.janilla.eshopweb.core.OrderItem;
import com.janilla.persistence.Persistence;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class OrderWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/order/my-orders")
	public Object show(ApplicationUser user) throws IOException {
		if (user == null)
			return URI.create("/Identity/Account/Login");
		var c = persistence.getCrud(Order.class);
		var d = persistence.getCrud(OrderItem.class);
		var i = c.read(c.filter("buyer", user.getUserName())).map(o -> {
			try {
				var t = d.read(d.filter("order", o.getId())).reduce(BigDecimal.ZERO,
						(a, b) -> a.add(b.getUnitPrice().multiply(BigDecimal.valueOf(b.getUnits()))), (a, b) -> a);
				return new View.Item(o, t);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).toList();
		return new View(i);
	}

	@Handle(method = "GET", uri = "/order/detail/(\\d+)")
	public Object show(long id, ApplicationUser user) throws IOException {
		if (user == null)
			return URI.create("/Identity/Account/Login");
		Order o;
		{
			var c = persistence.getCrud(Order.class);
			o = c.read(id);
		}
		if (!o.getBuyer().equals(user.getUserName()))
			throw new ForbiddenException();
		var c = persistence.getCrud(OrderItem.class);
		var i = c.read(c.filter("order", o.getId())).toList();
		var j = i.stream().map(x -> new Detail.Item(x, x.getUnitPrice().multiply(BigDecimal.valueOf(x.getUnits()))))
				.toList();
		var t = j.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b.total), (a, b) -> a);
		return new Detail(o, t, j);
	}

	@Render(template = "MyOrders.html")
	public record View(Collection<Item> items) {

		@Render(template = "MyOrders-Item.html")
		public record Item(Order order, BigDecimal total) {
		}
	}

	@Render(template = "Detail.html")
	public record Detail(Order order, BigDecimal total, Collection<Item> items) {

		@Render(template = "Detail-Item.html")
		public record Item(OrderItem item, BigDecimal total) {
		}
	}
}
