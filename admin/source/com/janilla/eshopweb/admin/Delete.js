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
class Delete {

	selector;

	engine;

	item;

	get display() {
		return this.item ? 'block' : 'none';
	}

	get layout() {
		return this.engine.stack[0].value;
	}

	render = async engine => {
		return await engine.match([this], async (_, o) => {
			this.engine = engine.clone();
			o.template = 'Delete';
		});
	}

	listen = () => {
		const e = this.selector();
		e.querySelectorAll('.close, .btn-secondary').forEach(x => x.addEventListener('click', this.handleCloseClick));
		e.querySelector('.btn-danger').addEventListener('click', this.handleDeleteClick);
	}

	refresh = async () => {
		this.selector().outerHTML = await this.engine.render({ value: this });
		this.listen();
	}

	handleCloseClick = async e => {
		delete this.item;
		await this.refresh();
		this.selector().dispatchEvent(new CustomEvent('deleteclose', { bubbles: true }));
	}

	handleDeleteClick = async e => {
		const s = await fetch(`${this.layout.api.url}/catalog-items/${this.item.id}`, {
			method: 'DELETE',
			headers: this.layout.api.headers
		});
		const j = await s.json();
		if (s.ok) {
			delete this.item;
			await this.refresh();
			this.selector().dispatchEvent(new CustomEvent('deleteclose', {
				bubbles: true,
				detail: { status: j.status }
			}));
		} else if (typeof j === 'string')
			alert(j);
	}
}

export default Delete;
