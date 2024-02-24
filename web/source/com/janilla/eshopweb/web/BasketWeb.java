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

import com.janilla.eshopweb.core.BasketItem;
import com.janilla.eshopweb.core.CatalogItem;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Parameter;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class BasketWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "/basket")
	public BasketPage getBasket(HttpExchange exchange) throws IOException {
		var b = ((CustomHttpExchange) exchange).getBasket(true);
		var c1 = persistence.getCrud(BasketItem.class);
		var c2 = persistence.getCrud(CatalogItem.class);
		var i = new ArrayList<Item>();
		for (var j = c1.read(c1.filter("basket", b.getId())).iterator(); j.hasNext();) {
			var i1 = j.next();
			var i2 = c2.read(i1.getCatalogItem());
			i.add(new Item(i.size(), i1, i2));
		}
		return new BasketPage(i);
	}

	@Handle(method = "POST", path = "/basket")
	public URI addItem(@Parameter(name = "id") Long id, HttpExchange exchange) throws IOException {
		var i = persistence.getCrud(CatalogItem.class).read(id);
		var b = ((CustomHttpExchange) exchange).getBasket(true);

		var j = new BasketItem();
		j.setCatalogItem(i.getId());
		j.setUnitPrice(i.getPrice());
		j.setQuantity(1);
		j.setBasket(b.getId());
		persistence.getCrud(BasketItem.class).create(j);

		return URI.create("/basket");
	}

	@Handle(method = "POST", path = "/basket/update")
	public URI update(BasketPage view) throws IOException {
		var c = persistence.getCrud(BasketItem.class);
		persistence.getDatabase().perform((ss, ii) -> {
			for (var i : view.items) {
				var q = i.basketItem.getQuantity();
				if (q > 0)
					c.update(i.basketItem.getId(), x -> x.setQuantity(q));
				else
					c.delete(i.basketItem.getId());
			}
			return null;
		}, true);
		return URI.create("/basket");
	}

	@Render(template = "Basket.html")
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

	@Render(template = "Basket-Empty.html")
	public record Empty() {
	}

	@Render(template = "Basket-Form.html")
	public record Form(List<Item> items) {

		public BigDecimal total() {
			return items.stream().map(x -> x.price()).reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	@Render(template = "Basket-Item.html")
	public record Item(int index, BasketItem basketItem, CatalogItem catalogItem) {

		public BigDecimal price() {
			return basketItem.getUnitPrice().multiply(BigDecimal.valueOf(basketItem.getQuantity()));
		}
	}
}
