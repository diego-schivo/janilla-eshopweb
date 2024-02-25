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
class NavMenu {

	selector;

	engine;

	get items() {
		const i = [
			{
				text: 'Home',
				href: 'admin',
				iconClass: 'oi-home'
			},
			{
				text: this.layout.user.username,
				href: 'account/profile',
				iconClass: 'oi-person'
			},
			{
				text: 'Logout',
				href: 'logout',
				iconClass: 'oi-account-logout'
			}
		];
		const j = i.find(x => `/${x.href}` === location.pathname);
		if (j) {
			j.activeClass = 'active';
			j.currentAttribute = 'aria-current="page"';
		}
		return i;
	}

	get layout() {
		return this.engine.stack[0].target;
	}

	render = async engine => {
		if (engine.isRendering(this)) {
			this.engine = engine.clone();
			return await engine.render(this, 'NavMenu');
		}

		if (engine.isRendering(this, 'items', true))
			return await engine.render(engine.target, 'NavMenu-Item');
	}

	listen = () => {
		this.selector().querySelector('[href="logout"]').addEventListener('click', this.handleLogoutClick);
	}

	handleLogoutClick = async e => {
		e.preventDefault();
		const s = await fetch('/user/logout', { method: 'POST' });
		if (s.ok)
			location.href = '/user/login';
	}
}

export default NavMenu;
