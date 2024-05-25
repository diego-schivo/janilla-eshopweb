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
import CatalogItem from './CatalogItem.js';
import Create from './Create.js';
import Delete from './Delete.js';
import Details from './Details.js';
import Edit from './Edit.js';

class Catalog {

	selector;

	engine;

	catalogItems;

	catalogTypes;

	catalogBrands;

	details;

	edit;

	create;

	delete;

	get layout() {
		return this.engine.stack[0].value;
	}

	render = async engine => {
		return await engine.match([this], async (_, o) => {
			this.engine = engine.clone();
			o.template = this.catalogItems ? 'Catalog2' : 'Catalog';
		}) || await engine.match([this, 'catalogType'], async (_, o) => {
			o.value = this.catalogTypes.find(x => x.id === engine.target.catalogType)?.name;
		}) || await engine.match([this, 'catalogBrand'], async (_, o) => {
			o.value = this.catalogBrands.find(x => x.id === engine.target.catalogBrand)?.name;
		}) || await engine.match([this, 'details'], async (_, o) => {
			this.details = new Details();
			this.details.selector = () => Array.from(this.selector().children).at(-4);
			o.value = this.details;
		}) || await engine.match([this, 'edit'], async (_, o) => {
			this.edit = new Edit();
			this.edit.selector = () => Array.from(this.selector().children).at(-3);
			o.value = this.edit;
		}) || await engine.match([this, 'create'], async (_, o) => {
			this.create = new Create();
			this.create.selector = () => Array.from(this.selector().children).at(-2);
			o.value = this.create;
		}) || await engine.match([this, 'delete'], async (_, o) => {
			this.delete = new Delete();
			this.delete.selector = () => this.selector().lastElementChild;
			o.value = this.delete;
		});
	}

	listen = () => {
		const e = this.selector();
		e.querySelector('.btn-primary')?.addEventListener('click', this.handleCreateClick);
		e.addEventListener('detailsopen', this.handleDetailsOpen);
		e.addEventListener('detailsclose', this.handleDetailsClose);
		e.addEventListener('editopen', this.handleEditOpen);
		e.addEventListener('editclose', this.handleEditClose);
		e.addEventListener('createclose', this.handleCreateClose);
		e.addEventListener('deleteopen', this.handleDeleteOpen);
		e.addEventListener('deleteclose', this.handleDeleteClose);
		if (!this.catalogItems)
			this.fetchItems().then(_ => {
				return this.engine.render({ value: this });
			}).then(h => {
				this.selector().outerHTML = h;
				this.listen();
			});
		this.catalogItems?.forEach(p => p.listen());
		this.details?.listen();
		this.edit?.listen();
		this.create?.listen();
		this.delete?.listen();
	}

	refresh = async () => {
		delete this.catalogItems;
		delete this.catalogTypes;
		delete this.catalogBrands;
		delete this.details;
		delete this.edit;
		delete this.create;
		delete this.delete;
		this.selector().outerHTML = await this.engine.render({ value: this });
		this.listen();
	}

	fetchItems = () => {
		return Promise.all([
			fetch(`${this.layout.api.url}/catalog-items`, {
				headers: this.layout.api.headers
			}).then(s => s.json()).then(j => {
				this.catalogItems = j.catalogItems.map((a, i) => {
					const q = new CatalogItem();
					q.selector = () => this.selector().querySelector('tbody').children[i];
					q.item = a;
					return q;
				});
				return j.pageCount;
			}),

			fetch(`${this.layout.api.url}/catalog-types`, {
				headers: this.layout.api.headers
			}).then(s => s.json()).then(j => this.catalogTypes = j.catalogTypes),

			fetch(`${this.layout.api.url}/catalog-brands`, {
				headers: this.layout.api.headers
			}).then(s => s.json()).then(j => this.catalogBrands = j.catalogBrands)
		]);
	}

	handleCreateClick = async e => {
		document.body.classList.add('body-no-overflow');
		this.create.item = {};
		await this.create.refresh();
	}

	handleDetailsOpen = async e => {
		document.body.classList.add('body-no-overflow');
		this.details.item = e.detail.item;
		await this.details.refresh();
	}

	handleDetailsClose = async e => {
		document.body.classList.remove('body-no-overflow');
	}

	handleEditOpen = async e => {
		document.body.classList.add('body-no-overflow');
		this.edit.item = e.detail.item;
		await this.edit.refresh();
	}

	handleEditClose = async e => {
		document.body.classList.remove('body-no-overflow');
		if (e.detail?.catalogItem)
			await this.refresh();
	}

	handleCreateClose = async e => {
		document.body.classList.remove('body-no-overflow');
		if (e.detail?.catalogItem)
			await this.refresh();
	}

	handleDeleteOpen = async e => {
		document.body.classList.add('body-no-overflow');
		this.delete.item = e.detail.item;
		await this.delete.refresh();
	}

	handleDeleteClose = async e => {
		document.body.classList.remove('body-no-overflow');
		if (e.detail?.status === 'Deleted')
			await this.refresh();
	}
}

export default Catalog;
