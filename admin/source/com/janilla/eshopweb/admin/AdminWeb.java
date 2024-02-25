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
package com.janilla.eshopweb.admin;

import java.util.Properties;

import com.janilla.web.Handle;
import com.janilla.web.Render;

public class AdminWeb {

	Properties configuration;

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	@Handle(method = "GET", path = "/admin")
	public Admin getAdmin() {
		var u = configuration.getProperty("eshopweb.admin.api.url");
		return new Admin(u);
	}

//	@Handle(method = "GET", path = "/admin.js")
//	public Script getScript() {
//		var u = configuration.getProperty("eshopweb.admin.api.url");
//		return new Script(u);
//	}

	@Render(template = "Admin.html")
	public record Admin(String apiUrl) {
	}

//	@Render(template = "admin.js")
//	public record Script(String apiUrl) {
//	}
}
