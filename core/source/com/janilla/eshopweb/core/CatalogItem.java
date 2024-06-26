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

import java.math.BigDecimal;
import java.net.URI;

import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

@Store
//@Index
public class CatalogItem {

	private long id;

	private String name;

	private String description;

	private BigDecimal price;

	private URI pictureUri;

	@Index
	private long catalogType;

	@Index
	private long catalogBrand;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public URI getPictureUri() {
		return pictureUri;
	}

	public void setPictureUri(URI pictureUri) {
		this.pictureUri = pictureUri;
	}

	public long getCatalogType() {
		return catalogType;
	}

	public void setCatalogType(long catalogType) {
		this.catalogType = catalogType;
	}

	public long getCatalogBrand() {
		return catalogBrand;
	}

	public void setCatalogBrand(long catalogBrand) {
		this.catalogBrand = catalogBrand;
	}
}
