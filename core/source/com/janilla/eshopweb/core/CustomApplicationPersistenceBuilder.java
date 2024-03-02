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
package com.janilla.eshopweb.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class CustomApplicationPersistenceBuilder extends ApplicationPersistenceBuilder {

	@Override
	public Persistence build() throws IOException {
		if (file == null) {
			Properties c;
			try {
				c = (Properties) Reflection.getter(application.getClass(), "configuration").invoke(application);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			var p = c.getProperty("eshopweb.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			file = Path.of(p);
		}
		var e = Files.exists(file);
		var p = super.build();
		if (!e) {
			for (var x : """
					Azure
					.NET
					Visual Studio
					SQL Server
					Other""".split("\n")) {
				var z = new CatalogBrand();
				z.setName(x);
				p.getDatabase().perform((ss, ii) -> p.getCrud(CatalogBrand.class).create(z), true);
			}
			for (var x : """
					Mug
					T-Shirt
					Sheet
					USB Memory Stick""".split("\n")) {
				var z = new CatalogType();
				z.setName(x);
				p.getDatabase().perform((ss, ii) -> p.getCrud(CatalogType.class).create(z), true);
			}
			for (var x : """
					2	2	.NET Bot Black Sweatshirt	.NET Bot Black Sweatshirt	19.5	/1.jpg
					1	2	.NET Black & White Mug	.NET Black & White Mug	8.5	/2.jpg
					2	5	Prism White T-Shirt	Prism White T-Shirt	12	/3.jpg
					2	2	.NET Foundation Sweatshirt	.NET Foundation Sweatshirt	12	/4.jpg
					3	5	Roslyn Red Sheet	Roslyn Red Sheet	8.5	/5.jpg
					2	2	.NET Blue Sweatshirt	.NET Blue Sweatshirt	12	/6.jpg
					2	5	Roslyn Red T-Shirt	Roslyn Red T-Shirt	12	/7.jpg
					2	5	Kudu Purple Sweatshirt	Kudu Purple Sweatshirt	8.5	/8.jpg
					1	5	Cup<T> White Mug	Cup<T> White Mug	12	/9.jpg
					3	2	.NET Foundation Sheet	.NET Foundation Sheet	12	/10.jpg
					3	2	Cup<T> Sheet	Cup<T> Sheet	8.5	/11.jpg
					2	5	Prism White TShirt	Prism White TShirt	12	/12.jpg""".split("\n")) {
				var y = x.split("\t");
				var z = new CatalogItem();
				z.setCatalogType(Long.valueOf(y[0]));
				z.setCatalogBrand(Long.valueOf(y[1]));
				z.setDescription(y[2]);
				z.setName(y[3]);
				z.setPrice(new BigDecimal(y[4]));
				z.setPictureUri(URI.create(y[5]));
				p.getDatabase().perform((ss, ii) -> p.getCrud(CatalogItem.class).create(z), true);
			}
			for (var x : """
					demouser@microsoft.com	Pass@word1
					admin@microsoft.com	Pass@word1	Administrators""".split("\n")) {
				var y = x.split("\t");
				var z = new ApplicationUser();
				z.setUserName(y[0]);
				z.setEmail(y[0]);
				ApplicationUser.setHashAndSalt(z, y[1]);
				z.setRoles(IntStream.range(2, y.length).mapToObj(i -> y[i]).toList());
				z.setTwoFactor(new ApplicationUser.TwoFactor(false, null, null));
				p.getDatabase().perform((ss, ii) -> p.getCrud(ApplicationUser.class).create(z), true);
			}
		}
		return p;
	}

	@Override
	protected Stream<String> getPackageNames() {
		return Stream.concat(super.getPackageNames(), Stream.of("com.janilla.eshopweb.core"));
	}
}
