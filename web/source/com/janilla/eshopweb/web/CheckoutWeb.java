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
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janilla.eshopweb.core.Address;
import com.janilla.eshopweb.core.Basket;
import com.janilla.eshopweb.core.BasketItem;
import com.janilla.eshopweb.core.CatalogItem;
import com.janilla.eshopweb.core.CatalogItemOrdered;
import com.janilla.eshopweb.core.Order;
import com.janilla.eshopweb.core.OrderItem;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class CheckoutWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/basket/checkout")
	public Checkout getCheckout(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		e.getUser(true);
		var b = e.getBasket(false);
		var c1 = persistence.getCrud(BasketItem.class);
		var c2 = persistence.getCrud(CatalogItem.class);
		var i = new ArrayList<Item>();
		for (var j = c1.read(c1.filter("basket", b.getId())).iterator(); j.hasNext();) {
			var i1 = j.next();
			var i2 = c2.read(i1.getCatalogItem());
			i.add(new Item(i1, i2));
		}
		return new Checkout(i);
	}

	@Handle(method = "POST", uri = "/basket/checkout")
	public URI pay(HttpExchange exchange) throws IOException {
		var e = (CustomHttpExchange) exchange;
		var b = e.getBasket(false);
		var o = new Order();
		o.setBuyer(b.getBuyer());
		o.setOrderDate(Instant.now());
		o.setShipToAddress(new Address("123 Main St.", "Kent", "OH", "United States", "44240"));
		Map<BasketItem, CatalogItem> m = new HashMap<>();
		{
			var c = persistence.getCrud(BasketItem.class);
			for (var j = c.read(c.filter("basket", b.getId())).iterator(); j.hasNext();) {
				var i1 = j.next();
				var i2 = persistence.getCrud(CatalogItem.class).read(i1.getCatalogItem());
				m.put(i1, i2);
			}
		}
		persistence.getDatabase().perform((ss, ii) -> {
			persistence.getCrud(Order.class).create(o);
			for (var f : m.entrySet()) {
				var i1 = f.getKey();
				var i2 = f.getValue();
				var i3 = new OrderItem();
				i3.setOrder(o.getId());
				i3.setItemOrdered(new CatalogItemOrdered(i2.getId(), i2.getName(), i2.getPictureUri()));
				i3.setUnitPrice(i1.getUnitPrice());
				i3.setUnits(i1.getQuantity());
				persistence.getCrud(OrderItem.class).create(i3);
			}
			persistence.getCrud(Basket.class).delete(b.getId());
			return null;
		}, true);
		return URI.create("/basket/success");
	}

	@Handle(method = "GET", uri = "/basket/success")
	public Success getSuccess() {
		return new Success();
	}

	@Render(template = "Checkout.html")
	public record Checkout(List<Item> items) implements Page {

		@Override
		public String title() {
			return "Basket";
		}

		public Empty empty() {
			return items.isEmpty() ? new Empty() : null;
		}

		public Form form() {
			return !items.isEmpty() ? new Form(items) : null;
		}
	}

	@Render(template = "Checkout-Empty.html")
	public record Empty() {
	}

	@Render(template = "Checkout-Form.html")
	public record Form(List<Item> items) {

		public BigDecimal total() {
			return items.stream().map(x -> x.price()).reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	@Render(template = "Checkout-Item.html")
	public record Item(BasketItem basketItem, CatalogItem catalogItem) {

		public BigDecimal price() {
			return basketItem.getUnitPrice().multiply(BigDecimal.valueOf(basketItem.getQuantity()));
		}
	}

	@Render(template = "Checkout-Success.html")
	public record Success() implements Page {

		@Override
		public String title() {
			return "Checkout Complete";
		}
	}
}
