const assert = require("assert").strict;
const sinon = require("sinon");

const wiki = require("../wiki");
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
				sinon.stub(Page, "findOne").returns(null);
				sinon.stub(Page, "build").returns({title});

				let view = await wiki.view(title);
				assert.equal(view.name, "not-exist");
				assert.deepEqual(view.data, {page: {title}});
			});
		});

		describe("if page exists", function() {
			let title = "viewpage";
			let page = {title};

			it("should return view view with the page", async function() {
				sinon.stub(Page, "findOne").returns(page);

				let view = await wiki.view(title);
				assert.equal(view.name, "view");
				assert.deepEqual(view.data, {page});
			});
		});
	});

	afterEach(sinon.restore);
});
