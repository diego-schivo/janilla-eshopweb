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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.janilla.eshopweb.core.BasketItem;
import com.janilla.eshopweb.core.CatalogItem;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.web.Handle;
import com.janilla.web.Bind;
import com.janilla.web.Render;

public class BasketWeb {

	Properties configuration;

	private Persistence persistence;

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "/basket")
	public BasketPage getBasket(HttpExchange exchange) throws IOException {
		var b = ((CustomExchange) exchange).getBasket(true);
		var c1 = persistence.crud(BasketItem.class);
		var c2 = persistence.crud(CatalogItem.class);
		var i = new ArrayList<Item>();
		for (var j = c1.read(c1.filter("basket", b.getId())).iterator(); j.hasNext();) {
			var i1 = j.next();
			var i2 = c2.read(i1.getCatalogItem());
			i.add(new Item(i.size(), i1, i2));
		}
		return new BasketPage(i);
	}

	@Handle(method = "POST", path = "/basket")
	public URI addItem(@Bind("id") long id, HttpExchange exchange) throws IOException {
		var b = ((CustomExchange) exchange).getBasket(true);
		var ii = persistence.crud(BasketItem.class).filter("basket", b.getId());
		var i = persistence.crud(BasketItem.class).read(ii).filter(x -> x.getCatalogItem() == id).findFirst()
				.orElse(null);
		if (i != null)
			persistence.crud(BasketItem.class).update(i.getId(), x -> {
				x.setQuantity(x.getQuantity() + 1);
				return x;
			});
		else {
			if (Boolean.parseBoolean(configuration.getProperty("eshopweb.live-demo")) && ii.length >= 10)
				throw new MethodBlockedException();
			var j = persistence.crud(CatalogItem.class).read(id);
			i = new BasketItem();
			i.setCatalogItem(j.getId());
			i.setUnitPrice(j.getPrice());
			i.setQuantity(1);
			i.setBasket(b.getId());
			persistence.crud(BasketItem.class).create(i);
		}

		return URI.create("/basket");
	}

	@Handle(method = "POST", path = "/basket/update")
	public URI update(BasketPage view) throws IOException {
		var c = persistence.crud(BasketItem.class);
		persistence.database().perform((ss, ii) -> {
			for (var i : view.items) {
				var q = i.basketItem.getQuantity();
				if (q > 0)
					c.update(i.basketItem.getId(), x -> {
						x.setQuantity(q);
						return x;
					});
				else
					c.delete(i.basketItem.getId());
			}
			return null;
		}, true);
		return URI.create("/basket");
	}

	@Render("Basket.html")
	public record BasketPage(List<Item> items) implements Page {

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

	@Render("Basket-Empty.html")
	public record Empty() {
	}

	@Render("Basket-Form.html")
	public record Form(List<Item> items) {

		public BigDecimal total() {
			return items.stream().map(x -> x.price()).reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	@Render("Basket-Item.html")
	public record Item(int index, BasketItem basketItem, CatalogItem catalogItem) {

		public BigDecimal price() {
			return basketItem.getUnitPrice().multiply(BigDecimal.valueOf(basketItem.getQuantity()));
		}
	}
}
