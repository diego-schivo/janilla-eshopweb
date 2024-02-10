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
package com.janilla.eshopweb.api;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import com.janilla.eshopweb.core.CatalogItem;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.Handle;

public class CatalogItemApi {

	Persistence persistence;

	public Persistence getPersistence() {
		return persistence;
	}

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", uri = "/api/catalog-items")
	public Object list() throws IOException {
		var c = persistence.getCrud(CatalogItem.class);
		var i = c.list();
		var j = c.read(i).toList();
		return Map.of("catalogItems", j, "pageCount", 1);
	}

	@Handle(method = "POST", uri = "/api/catalog-items")
	public Object create(CatalogItem item) throws IOException {
		item.setPictureUri(URI.create("/eCatalog-item-default.png"));
		var c = persistence.getCrud(CatalogItem.class);
		var i = persistence.getDatabase().performTransaction(() -> c.create(item));
		return Map.of("catalogItem", i);
	}

	@Handle(method = "PUT", uri = "/api/catalog-items")
	public Object update(CatalogItem item) throws IOException {
		var c = persistence.getCrud(CatalogItem.class);
		var i = persistence.getDatabase().performTransaction(() -> c.update(item.getId(),
				x -> Reflection.copy(item, x, y -> !Set.of("id", "pictureUri").contains(y))));
		return Map.of("catalogItem", i);
	}

	@Handle(method = "DELETE", uri = "/api/catalog-items/(\\d+)")
	public Object delete(long id) throws IOException {
		var c = persistence.getCrud(CatalogItem.class);
		persistence.getDatabase().performTransaction(() -> c.delete(id));
		return Map.of("status", "Deleted");
	}
}
