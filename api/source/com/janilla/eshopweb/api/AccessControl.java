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

import java.util.Properties;
import java.util.stream.Collectors;

import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.HttpResponse.Status;
import com.janilla.web.AnnotationDrivenToMethodInvocation;
import com.janilla.web.Handle;

public class AccessControl {

	Properties configuration;

	AnnotationDrivenToMethodInvocation toInvocation;

//	public Properties getConfiguration() {
//		return configuration;
//	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

//	public AnnotationDrivenToMethodInvocation getToInvocation() {
//		return toInvocation;
//	}

	public void setToInvocation(AnnotationDrivenToMethodInvocation toInvocation) {
		this.toInvocation = toInvocation;
	}

	@Handle(method = "OPTIONS", uri = "/api/(.*)")
	public void allow(HttpRequest request, HttpResponse response) {
		var o = configuration.getProperty("eshopweb.api.cors.origin");
		var s = toInvocation.getValueAndGroupsStream(request).flatMap(w -> w.value().methods().stream())
				.map(m -> m.getAnnotation(Handle.class).method()).collect(Collectors.toSet());
		var m = s.contains(null) ? "*" : s.stream().collect(Collectors.joining(", "));
		var h = configuration.getProperty("eshopweb.api.cors.headers");

		response.setStatus(new Status(204, "No Content"));
		response.getHeaders().set("Access-Control-Allow-Origin", o);
		response.getHeaders().set("Access-Control-Allow-Methods", m);
		response.getHeaders().set("Access-Control-Allow-Headers", h);
	}
}
