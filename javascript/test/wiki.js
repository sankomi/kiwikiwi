const assert = require("assert").strict;
const sinon = require("sinon");
const {replace, fake, restore} = sinon;
const rewire = require("rewire");

const wiki = rewire("../wiki");
const {Page} = require("../models");

describe("wiki.js", function() {
	describe("index()", function() {
		it("should return index view", function() {
			let view = wiki.index();
			assert.equal(view.name, "index");
		});
	});

	describe("view(title)", function() {
		describe("if page does not exist", function() {
			let title = "viewnopage";

			it("should return not-exist view with title", async function() {
				let findOne = replace(Page, "findOne", fake(args => null));

				let view = await wiki.view(title);
				assert.equal(findOne.callCount, 1);
				assert.equal(view.name, "not-exist");
				assert.deepEqual(view.data, {page: {title}});
			});
		});

		describe("if page exists", function() {
			let title = "viewpage";
			let page = {title};

			it("should return view view with the page", async function() {
				let findOne = replace(Page, "findOne", fake(args => page));

				let view = await wiki.view(title);
				assert.equal(findOne.callCount, 1);
				assert.equal(view.name, "view");
				assert.deepEqual(view.data, {page});
			});
		});
	});

	describe("editView(title)", function() {
		describe("if page does not exist", function() {
			let title = "editviewnopage";

			it("should return edit view with title and new title", async function() {
				let findOne = replace(Page, "findOne", fake(args => null));
				let build = replace(Page, "build", fake(args => ({title: args.title})));

				let view = await wiki.editView(title);
				assert.equal(findOne.callCount, 1);
				assert.equal(build.callCount, 1);
				assert.equal(view.name, "edit");
				assert.deepEqual(view.data, {page: {title, newTitle: title}});
			});
		});

		describe("if page exists", function() {
			let title = "editviewpage";
			let page = {title};

			it("should return edit view with the page", async function() {
				let findOne = replace(Page, "findOne", fake(args => page));

				let view = await wiki.editView(title);
				assert.equal(findOne.callCount, 1);
				assert.equal(view.name, "edit");
				assert.deepEqual(view.data, {page: {...page, newTitle: title}});
			});
		});
	});

	describe("editEdit(title, newTitle, summary, content)", function() {
		describe("if updated", function() {
			let title = "editeditupdated";
			let newTitle = "editeditupdatednewtitle";
			let summary = "editeditupdatedsummary";
			let content = "editeditupdatedcontent";
			let updated = {title: newTitle};

			it("should redirect to view page", async function() {
				let update = wire(wiki, "update", args => updated);

				let view = await wiki.editEdit(title, newTitle, summary, content);
				assert.equal(update.callCount, 1);
				assert.deepEqual(view, {redirect: `/wiki/${newTitle}`});
			});
		});
	});

	afterEach(() => {
		restore();
		unwire();
	});
});

const reverts = [];

function wire(mod, name, func) {
	let obj = {[name]: mod.__get__(name)};
	let rep = replace(obj, name, fake(func));
	reverts.push(mod.__set__(name, rep));

	return rep;
}

function unwire() {
	reverts.forEach(revert => revert());
	reverts.length = 0;
}
