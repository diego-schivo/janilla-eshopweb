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

import com.janilla.eshopweb.core.BasketItem;
import com.janilla.frontend.RenderEngine;
import com.janilla.http.HttpExchange;
import com.janilla.web.TemplateHandlerFactory;

public class CustomTemplateHandlerFactory extends TemplateHandlerFactory {

	@Override
	protected void render(RenderEngine.Entry input, HttpExchange exchange) {
		var o = input.getValue();
		if (o instanceof Page p)
			input = RenderEngine.Entry.of(null, toLayout(p, exchange), null);
		super.render(input, exchange);
	}

	static Layout toLayout(Page p, HttpExchange exchange) {
		var e = (CustomExchange) exchange;
		var b = e.getBasket(false);
		var q = 0;
		if (b != null) {
			var c = e.persistence.crud(BasketItem.class);
			var i = c.filter("basket", b.getId());
			q = c.read(i).mapToInt(BasketItem::getQuantity).sum();
		}
		return new Layout(e.getUser(false), q, p);
	}
}
