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
import Catalog from './Catalog.js';
import NavMenu from './NavMenu.js';
import Rendering from './Rendering.js';
import Toast from './Toast.js';

class MainLayout {

	selector;

	user;

	api;

//	get administrator() {
//		return this.user?.claims.some(x => x.type === this.user.roleClaimType && x.value === 'Administrators');
//	}

	run = async () => {
		const s = await fetch('/user');
		this.user = s.ok ? await s.json() : null;
		if (this.user) {
			this.api = {
				url: 'http://127.0.0.1:8081/api',
				headers: {
					Authorization: `Bearer ${this.user.token}`
				}
			};
			const r = new Rendering();
			this.selector().innerHTML = await r.render(this, 'MainLayout');
			this.listen();
		} else
			location.href = '/user/login?returnUrl=%2fAdmin';
	}

	render = async (key, rendering) => {
		switch (key) {
			case 'sidebar':
				return this.user.roles.includes('Administrators') ? await rendering.render(this, 'MainLayout-sidebar') : null;

			case 'navMenu':
				this.navMenu = new NavMenu();
				this.navMenu.selector = () => this.selector().querySelector('.sidebar').firstElementChild;
				return this.navMenu;

			case 'toast':
				this.toast = new Toast();
				this.toast.selector = () => this.selector().querySelector('.content').firstElementChild;
				return this.toast;

			case 'catalog':
				if (this.user.roles.includes('Administrators')) {
					this.catalog = new Catalog();
					this.catalog.selector = () => this.selector().querySelector('.content').lastElementChild;
					return this.catalog;
				} else
					return await rendering.render(this, 'MainLayout-unauthorized');
		}
	}

	listen = () => {
		this.navMenu?.listen();
		this.toast.listen();
		this.catalog?.listen();
	}
}

export default MainLayout;
