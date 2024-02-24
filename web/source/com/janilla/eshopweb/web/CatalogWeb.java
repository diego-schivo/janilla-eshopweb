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
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.eshopweb.core.CatalogBrand;
import com.janilla.eshopweb.core.CatalogItem;
import com.janilla.eshopweb.core.CatalogType;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Parameter;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class CatalogWeb {

	private Persistence persistence;

	public void setPersistence(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET", path = "/")
	public Catalog getCatalog(@Parameter(name = "brand") long brand, @Parameter(name = "type") long type,
			@Parameter(name = "page") int page) throws IOException {
		var c1 = persistence.getCrud(CatalogBrand.class);
		var b = Stream
				.concat(Stream.of(new Option(null, "All", false)),
						c1.read(c1.filter("name"))
								.map(x -> new Option(String.valueOf(x.getId()), x.getName(), x.getId() == brand)))
				.toList();

		var c2 = persistence.getCrud(CatalogType.class);
		var t = Stream
				.concat(Stream.of(new Option(null, "All", false)),
						c2.read(c2.filter("name"))
								.map(x -> new Option(String.valueOf(x.getId()), x.getName(), x.getId() == type)))
				.toList();

		var f = new Filters(b, t);

		var c3 = persistence.getCrud(CatalogItem.class);
		var p = c3.filter(Map.of("catalogBrand", brand > 0 ? new Object[] { brand } : new Object[0], "catalogType",
				type > 0 ? new Object[] { type } : new Object[0]), page * 10, 10);

		var q = new Pager(new CurrentAndTotal(p.ids().length, p.total()),
				new CurrentAndTotal(page + 1, Math.ceilDiv(p.total(), 10)), new PageAndEnabled(page - 1, page > 0),
				new PageAndEnabled(page + 1, (page + 1) * 10 < p.total()));
		var i = c3.read(p.ids()).toList();

		return new Catalog(f, q, i);
	}

	@Render(template = "Catalog.html")
	public record Catalog(Filters filters, Pager pager, List<@Render(template = "CatalogItem.html") CatalogItem> items)
			implements Page {

		@Override
		public String title() {
			return "Catalog";
		}
	}

	@Render(template = "Catalog-Filters.html")
	public record Filters(List<Option> brandOptions, List<Option> typeOptions) {
	}

	@Render(template = """
			<option value="${value}" #{selectedAttribute}>${text}</option>
			""")
	public record Option(String value, String text, boolean selected) {

		public String selectedAttribute() {
			return selected ? "selected" : null;
		}
	}

	@Render(template = "Catalog-Pager.html")
	public record Pager(CurrentAndTotal items, CurrentAndTotal pages, PageAndEnabled previous, PageAndEnabled next) {
	}

	public record CurrentAndTotal(long current, long total) {
	}

	public record PageAndEnabled(int page, boolean enabled) {

		public URI uri() {
			return URI.create("/?page=" + page);
		}

		public String enabledClass() {
			return enabled ? null : "is-disabled";
		}
	}
}
