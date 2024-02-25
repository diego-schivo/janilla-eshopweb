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
class CatalogItem {

	selector;

	item;

	render = async engine => {
		if (engine.isRendering(this))
			return await engine.render(this, 'CatalogItem');
	}

	listen = () => {
		const e = this.selector();
		e.addEventListener('click', this.handleClick);
		e.querySelector('.btn-primary').addEventListener('click', this.handleEditClick);
		e.querySelector('.btn-danger').addEventListener('click', this.handleDeleteClick);
	}

	handleClick = e => {
		this.selector().dispatchEvent(new CustomEvent('detailsopen', {
			bubbles: true,
			detail: { item: this.item }
		}));
	}

	handleEditClick = e => {
		e.stopPropagation();
		this.selector().dispatchEvent(new CustomEvent('editopen', {
			bubbles: true,
			detail: { item: this.item }
		}));
	}

	handleDeleteClick = e => {
		e.stopPropagation();
		this.selector().dispatchEvent(new CustomEvent('deleteopen', {
			bubbles: true,
			detail: { item: this.item }
		}));
	}
}

export default CatalogItem;
