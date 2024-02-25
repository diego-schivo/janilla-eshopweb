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
class Details {

	selector;

	engine;

	item;

	get display() {
		return this.item ? 'block' : 'none';
	}

	render = async engine => {
		if (engine.isRendering(this)) {
			this.engine = engine.clone();
			return await engine.render(this, 'Details');
		}
	}

	listen = () => {
		var e = this.selector();
		e.querySelectorAll('.close, .btn-secondary').forEach(x => x.addEventListener('click', this.handleCloseClick));
		e.querySelector('.btn-primary').addEventListener('click', this.handleEditClick);
	}

	refresh = async () => {
		this.selector().outerHTML = await this.engine.render(this, 'Details');
		this.listen();
	}

	handleCloseClick = async e => {
		delete this.item;
		await this.refresh();
		this.selector().dispatchEvent(new CustomEvent('detailsclose', { bubbles: true }));
	}

	handleEditClick = async e => {
		const i = this.item;
		delete this.item;
		await this.refresh();
		this.selector().dispatchEvent(new CustomEvent('editopen', {
			bubbles: true,
			detail: { item: i }
		}));
	}
}

export default Details;
