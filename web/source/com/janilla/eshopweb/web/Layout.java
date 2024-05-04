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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.janilla.eshopweb.core.ApplicationUser;
import com.janilla.frontend.RenderEngine;
import com.janilla.frontend.Renderer;
import com.janilla.web.Render;

@Render("Layout.html")
public record Layout(ApplicationUser user, @Render("Layout-Basket.html") int basket, Page page)
		implements Renderer {

	public Login login() {
		return user == null ? new Login() : null;
	}

	public Form form() {
		return user != null ? new Form(user) : null;
	}

	protected static Format currencyFormat = new DecimalFormat("0.00");

	protected static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss xxx")
			.withZone(ZoneOffset.UTC);

	@Override
	public boolean evaluate(RenderEngine engine) {
		record A(BigDecimal decimal) {
		}
		record B(Instant instant) {
		}
		return engine.match(A.class, (x, y) -> {
			if (x.decimal != null)
				y.setValue(currencyFormat.format(x.decimal));
		}) || engine.match(B.class, (x, y) -> {
			if (x.instant != null)
				y.setValue(dateTimeFormatter.format(x.instant));
		});
	}

	@Render("Layout-Login.html")
	public record Login() {
	}

	@Render("Layout-Form.html")
	public record Form(ApplicationUser user) {

		public Admin admin() {
			var r = user.getRoles();
			return r != null && r.contains("Administrators") ? new Admin() : null;
		}

		@Render("Layout-Form-Admin.html")
		public record Admin() {
		}
	}
}
