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
import java.math.BigDecimal;
import java.util.List;

import com.janilla.eshopweb.core.Order;
import com.janilla.eshopweb.core.OrderItem;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.web.ForbiddenException;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class OrderWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "/order/history")
	public History getHistory(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		var c = persistence.getCrud(Order.class);
		var d = persistence.getCrud(OrderItem.class);
		var i = c.read(c.filter("buyer", u.getUserName())).map(o -> {
			var t = d.read(d.filter("order", o.getId())).reduce(BigDecimal.ZERO,
					(a, b) -> a.add(b.getUnitPrice().multiply(BigDecimal.valueOf(b.getUnits()))), (a, b) -> a);
			return new History.Item(o, t);
		}).toList();
		return new History(i);
	}

	@Handle(method = "GET", path = "/order/detail/(\\d+)")
	public Detail getDetail(long id, HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var u = e.getUser(true);
		Order o;
		{
			var c = persistence.getCrud(Order.class);
			o = c.read(id);
		}
		if (!o.getBuyer().equals(u.getUserName()))
			throw new ForbiddenException();
		var c = persistence.getCrud(OrderItem.class);
		var i = c.read(c.filter("order", o.getId())).toList();
		var j = i.stream().map(x -> new Detail.Item(x, x.getUnitPrice().multiply(BigDecimal.valueOf(x.getUnits()))))
				.toList();
		var t = j.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b.total), (a, b) -> a);
		return new Detail(o, t, j);
	}

	@Render(template = "Order-History.html")
	public record History(List<Item> items) implements Page {

		@Override
		public String title() {
			return "My Order History";
		}

		@Render(template = "Order-History-Item.html")
		public record Item(Order order, BigDecimal total) {
		}
	}

	@Render(template = "Order-Detail.html")
	public record Detail(Order order, BigDecimal total, List<Item> items) implements Page {

		@Override
		public String title() {
			return "Order Detail";
		}

		@Render(template = "Order-Detail-Item.html")
		public record Item(OrderItem item, BigDecimal total) {
		}
	}
}
