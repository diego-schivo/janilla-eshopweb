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
class Edit {

	selector;

	rendering;

	data;

	validationMessages;
	
	set item(x) {
		this.data = new FormData();
		Object.entries(x).forEach(([k, v]) => this.data.append(k, v?.toString()));
	}

	get display() {
		return this.data ? 'block' : 'none';
	}

	get catalog() {
		return this.rendering.stack[1].object;
	}

	render = async (key, rendering) => {
		switch (key) {
			case undefined:
				this.rendering = rendering.clone();
				return await rendering.render(this, 'Edit');

			case 'catalogBrandOptions':
				return this.catalog.catalogBrands?.map(x => ({
					value: x.id,
					text: x.name,
					selected: x.id == this.data?.get('catalogBrand')
				}));

			case 'catalogTypeOptions':
				return this.catalog.catalogTypes?.map(x => ({
					value: x.id,
					text: x.name,
					selected: x.id == this.data?.get('catalogType')
				}));

			case 'selectedAttribute':
				return rendering.object.selected ? 'selected' : '';
		}

		switch (rendering.stack.at(-2)?.key) {
			case 'catalogBrandOptions':
			case 'catalogTypeOptions':
				return await rendering.render(rendering.object[key], 'Create-option');
			case 'validationClasses':
				return this.validationMessages?.hasOwnProperty(key) ? 'invalid' : 'valid';
			case 'validationAttributes':
				return this.validationMessages?.hasOwnProperty(key) ? 'aria-invalid="true"' : null;
			case 'validationMessages':
				return this.validationMessages?.hasOwnProperty(key) ? await rendering.render(rendering.object[key], 'Create-validationMessage') : null;
		}
	}

	listen = () => {
		this.selector().querySelectorAll('.close, .btn-secondary').forEach(x => x.addEventListener('click', this.handleCloseClick));
		this.selector().querySelector('form').addEventListener('submit', this.handleFormSubmit);
	}

	refresh = async () => {
		this.selector().outerHTML = await this.rendering.render(this, 'Edit');
		this.listen();
	}

	handleCloseClick = async e => {
		delete this.data;
		await this.refresh();
		this.selector().dispatchEvent(new CustomEvent('editclose', { bubbles: true }));
	}

	handleFormSubmit = async e => {
		e.preventDefault();
		this.data = new FormData(e.currentTarget);
		let o = {};
		this.data.forEach((v, k) => {
			switch (k) {
				case 'name':
					if (v === '')
						o[k] = 'The Name field is required';
					break;
				case 'description':
					if (v === '')
						o[k] = 'The Description field is required';
					break;
				case 'price':
					if (!/^\d+(\.\d{0,2})?$/.test(v))
						o[k] = 'The field Price must be a positive number with maximum two decimals.';
					else {
						const f = parseFloat(v);
						if (f < 0.01 || f > 1000)
							o[k] = 'The field Price must be between 0.01 and 1000.';
					}
					break;
			}
		});
		if (Object.keys(o).length) {
			this.validationMessages = o;
			await this.refresh();
			return;
		}
		o = {};
		this.data.forEach((v, k) => {
			switch (k) {
				case 'catalogType':
				case 'catalogBrand':
					o[k] = parseInt(v, 10);
					break;
				case 'price':
					o[k] = parseFloat(v);
					break;
				default:
					o[k] = v;
					break;
			}
		});
		const s = await fetch('/api/catalog-items', {
			method: 'PUT',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(o)
		});
		if (s.ok) {
			delete this.data;
			await this.refresh();
			this.selector().dispatchEvent(new CustomEvent('editclose', {
				bubbles: true,
				detail: { catalogItem: (await s.json()).catalogItem }
			}));
		}
	}
}

export default Edit;
