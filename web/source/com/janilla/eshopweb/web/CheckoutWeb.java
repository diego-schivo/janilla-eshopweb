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
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.janilla.eshopweb.core.Address;
import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.eshopweb.core.Basket;
import com.janilla.eshopweb.core.BasketItem;
import com.janilla.eshopweb.core.CatalogItem;
import com.janilla.eshopweb.core.CatalogItemOrdered;
import com.janilla.eshopweb.core.Order;
import com.janilla.eshopweb.core.OrderItem;
import com.janilla.persistence.Persistence;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class CheckoutWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/Basket/Checkout")
	public Object show(Basket basket) throws IOException {
		var c = persistence.getCrud(BasketItem.class);
		var i = c.read(c.filter("basket", basket.getId())).map(x -> {
			try {
				var y = persistence.getCrud(CatalogItem.class).read(x.getCatalogItem());
				return new View.Item(x, y);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).toList();
		return new View(basket, i);
	}

	@Handle(method = "POST", uri = "/Basket/Checkout")
	public Object pay(Basket basket, ApplicationUser user) throws IOException {
		var o = new Order();
		o.setBuyer(user.getUserName());
		o.setOrderDate(Instant.now());
		o.setShipToAddress(new Address("123 Main St.", "Kent", "OH", "United States", "44240"));
		Map<BasketItem, CatalogItem> m;
		{
			var c = persistence.getCrud(BasketItem.class);
			var d = persistence.getCrud(CatalogItem.class);
			m = c.read(c.filter("basket", basket.getId())).collect(Collectors.toMap(x -> x, x -> {
				try {
					return d.read(x.getCatalogItem());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
		}
		persistence.getDatabase().performTransaction(() -> {
			persistence.getCrud(Order.class).create(o);
			for (var e : m.entrySet()) {
				var bi = e.getKey();
				var ci = e.getValue();
				var oi = new OrderItem();
				oi.setOrder(o.getId());
				oi.setItemOrdered(new CatalogItemOrdered(ci.getId(), ci.getName(), ci.getPictureUri()));
				oi.setUnitPrice(bi.getUnitPrice());
				oi.setUnits(bi.getQuantity());
				persistence.getCrud(OrderItem.class).create(oi);
			}
			persistence.getCrud(Basket.class).delete(basket.getId());
		});
		return new Success();
	}

	@Render(template = "Checkout.html")
	public record View(Basket basket, Collection<Item> items) {

		@Render(template = "Checkout-Item.html")
		public record Item(BasketItem basketItem, CatalogItem catalogItem) {
		}
	}

	@Render(template = "Success.html")
	public record Success() {
	}
}
