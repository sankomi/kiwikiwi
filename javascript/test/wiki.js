const assert = require("assert").strict;
const sinon = require("sinon");
const {replace, fake, restore} = sinon;
const rewire = require("rewire");

const wiki = rewire("../wiki");
const {Page, History} = require("../models");
const {TitleDuplicateError, PageLockError} = require("../error");

describe("wiki.js", function() {
	describe("index()", function() {
		it("should return index view", function() {
			let view = wiki.index();
			assert.equal(view.name, "index");
		});
	});

	describe("view(title)", function() {
		describe("if page does not exist", function() {
			let prefix = "viewnopage"
			let title = prefix + "title";

			it("should return not-exist view with title", async function() {
				let findOne = replace(Page, "findOne", fake(args => null));

				let view = await wiki.view(title);
				assert.equal(findOne.callCount, 1);
				assert.equal(view.name, "not-exist");
				assert.deepEqual(view.data, {page: {title}});
			});
		});

		describe("if page exists", function() {
			let prefix = "viewpage";
			let title = prefix + "title";
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
			let prefix = "editviewnopage";
			let title = prefix + "title";

			it("should return edit view with title and new title", async function() {
				let findOne = replace(Page, "findOne", fake(args => null));
				let build = replace(Page, "build", fake(args => ({...args})));

				let view = await wiki.editView(title);
				assert.equal(findOne.callCount, 1);
				assert.equal(build.callCount, 1);
				assert.equal(view.name, "edit");
				assert.deepEqual(view.data, {page: {title, newTitle: title}});
			});
		});

		describe("if page exists", function() {
			let prefix = "editviewpage";
			let title = prefix + "title";
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
			let prefix = "editeditupdated";
			let title = prefix + "title";
			let newTitle = prefix + "newtitle";
			let summary = prefix + "summary";
			let content = prefix + "content";
			let updated = {title: newTitle};

			it("should redirect to view page", async function() {
				let update = wireFunc(wiki, "update", args => updated);

				let view = await wiki.editEdit(title, newTitle, summary, content);
				assert.equal(update.callCount, 1);
				assert.deepEqual(view, {redirect: `/wiki/${newTitle}`});
			});
		});

		describe("if update fails due to duplicate title", function() {
			let prefix = "editeditupdateduplicatetitle";
			let title = prefix + "title";
			let newTitle = prefix + "newtitle";
			let summary = prefix + "summary";
			let content = prefix + "content";
			let page = {title, summary, content};

			it("should return edit view with edit details", async function() {
				let update = wireError(wiki, "update", TitleDuplicateError, "page with new title already exists");
				let build = replace(Page, "build", fake(args => ({...args})));

				let view = await wiki.editEdit(title, newTitle, summary, content);
				assert.equal(update.callCount, 1);
				assert.equal(build.callCount, 1);
				assert.equal(view.name, "edit");
				assert.deepEqual(view.data, {page: {...page, newTitle: title}});
			});
		});

		describe("if update fails due to page lock", function() {
			let prefix = "editeditupdatepagelock";
			let title = prefix + "title";
			let newTitle = prefix + "newtitle";
			let summary = prefix + "summary";
			let content = prefix + "content";
			let page = {title, summary, content};

			it("should return edit view with edit details", async function() {
				let update = wireError(wiki, "update", PageLockError, "page is locked");
				let build = replace(Page, "build", fake(args => ({...args})));

				let view = await wiki.editEdit(title, newTitle, summary, content);
				assert.equal(update.callCount, 1);
				assert.equal(build.callCount, 1);
				assert.equal(view.name, "edit");
				assert.deepEqual(view.data, {page: {...page, newTitle: title}});
			});
		});
	});

	describe("update(title, newTitle, summary, content)", function() {
		let update = wiki.__get__("update");

		describe("if page with new title exists", function() {
			let prefix = "updateduplicatetitle";
			let title = prefix + "title";
			let newTitle = prefix + "newtitle";
			let summary = prefix + "summary";
			let content = prefix + "content";
			let page = {title, content};
			let existing = {title: newTitle, content};

			it("should throw title duplicate error", async function() {
				let findOne = replace(Page, "findOne", fake(args => {
					switch(args.where.title) {
						case title:
							return page;
						case newTitle:
							return existing;
						default:
							return null;
					}
				}));

				await assert.rejects(
					async () => await update(title, newTitle, summary, content),
					TitleDuplicateError,
					"page with title already exists",
				);
				assert.equal(findOne.callCount, 2);
			});
		});

		describe("if page does not exist", function() {
			let prefix = "updatenopage";
			let title = prefix + "title";
			let summary = prefix + "summary";
			let content = prefix + "content";

			describe("and title is same as new title", function() {
				let newTitle = title;

				it("should return new page with title and content", async function() {
					let pageFindOne = replace(Page, "findOne", fake(() => null));
					let pageSave = fake();
					let pageBuild = replace(Page, "build", fake(args => ({...args, save: pageSave})));
					let historySave = fake(async () => null);
					let historySetPage = fake();
					let historyBuild = replace(History, "build", fake(args => ({...args, save: historySave, setPage: historySetPage})));
					let getPatch = wireFunc(wiki, "getPatch", args => "patch");

					let updated = await update(title, newTitle, summary, content);
					[pageFindOne, pageBuild, pageSave].forEach(func => assert.equal(func.callCount, 1));
					[historyBuild, historySave, historySetPage].forEach(func => assert.equal(func.callCount, 1));
					assert.equal(getPatch.callCount, 2);
					assert.equal(updated.title, newTitle);
					assert.equal(updated.content, content);
				});
			});

			describe("and title is not same as new title", function() {
				let newTitle = prefix + "newtitle";

				it("should return new page with title and content", async function() {
					let pageFindOne = replace(Page, "findOne", fake(args => null));
					let pageSave = fake();
					let pageBuild = replace(Page, "build", fake(args => ({...args, save: pageSave})));
					let historySave = fake(async () => null);
					let historySetPage = fake();
					let historyBuild = replace(History, "build", fake(args => ({...args, save: historySave, setPage: historySetPage})));
					let getPatch = wireFunc(wiki, "getPatch", args => "patch");

					let updated = await update(title, newTitle, summary, content);
					assert.equal(pageFindOne.callCount, 2);
					[pageBuild, pageSave].forEach(func => assert.equal(func.callCount, 1));
					[historyBuild, historySave, historySetPage].forEach(func => assert.equal(func.callCount, 1));
					assert.equal(getPatch.callCount, 2);
					assert.equal(updated.title, newTitle);
					assert.equal(updated.content, content);
				});
			});
		});

		describe("if page is not locked", function() {
			let prefix = "updatenotlocked";
			let title = prefix + "title";
			let newTitle = title;
			let summary = prefix + "summary";
			let content = prefix + "content";

			describe("and page cannot be locked", function() {
				it("should throw page lock error", async function() {
					let page = {title, content};
					let findOne = replace(Page, "findOne", fake(args => {
						let where = args.where;
						if (where.title === title) {
							if (where.lock || where.lockId)  {
								return null;
							} else {
								return page;
							}
						} else {
							return null;
						}
					}));
					let pageUpdate = replace(Page, "update", fake());

					await assert.rejects(
						async () => await update(title, newTitle, summary, content),
						PageLockError,
						"page is locked",
					);
					assert.equal(findOne.callCount, 2);
					assert.equal(pageUpdate.callCount, 1);
				});
			});

			describe("and page can be locked", function() {
				it("should return updated page", async function() {
					let historyEvent = randomInt();
					let pageSave = fake();
					let page = {title, content, save: pageSave};
					let pageFindOne = replace(Page, "findOne", fake(args => {
						let where = args.where;
						if (where.title === title) {
							if (where.lock || where.lockId)  {
								page.lock = where.lock;
								page.lockId = where.lockId;
								page.histories = [{event: historyEvent}];
								return page;
							} else {
								return page;
							}
						} else {
							return null;
						}
					}));
					let pageUpdate = replace(Page, "update", fake());
					let getPatch = wireFunc(wiki, "getPatch", args => "patch");
					let historySave = fake(async () => null);
					let historySetPage = fake();
					let historyBuild = replace(History, "build", fake(args => ({...args, save: historySave, setPage: historySetPage})));

					let locked = await update(title, newTitle, summary, content);
					assert.equal(pageFindOne.callCount, 2);
					[pageUpdate, pageSave].forEach(func => assert.equal(func.callCount, 1));
					[historyBuild, historySave, historySetPage].forEach(func => assert.equal(func.callCount, 1));
					assert.equal(getPatch.callCount, 2)
					assert.equal(locked.title, newTitle);
					assert.equal(locked.content, content);
					assert.equal(locked.lock, null);
					assert.equal(locked.lockId, null);
				});
			});
		});

		describe("if page is locked", function() {
			let prefix = "updatelocked";
			let title = prefix + "title";
			let newTitle = title;
			let summary = prefix + "summary";
			let content = prefix + "content";

			describe("and lock is expired", function() {
				it("should return updated page", async function() {
					let lockId = randomInt();
					let lock = new Date(+new Date() - 100000);
					let historyEvent = randomInt();
					let pageSave = fake();
					let page = {title, content, lockId, lock, save: pageSave};
					let pageFindOne = replace(Page, "findOne", fake(args => {
						let where = args.where;
						if (where.title === title) {
							if (where.lock || where.lockId)  {
								page.lock = where.lock;
								page.lockId = where.lockId;
								page.histories = [{event: historyEvent}];
								return page;
							} else {
								return page;
							}
						} else {
							return null;
						}
					}));
					let pageUpdate = replace(Page, "update", fake());
					let getPatch = wireFunc(wiki, "getPatch", args => "patch");
					let historySave = fake(async () => null);
					let historySetPage = fake();
					let historyBuild = replace(History, "build", fake(args => ({...args, save: historySave, setPage: historySetPage})));

					let locked = await update(title, newTitle, summary, content);
					[pageFindOne, pageSave].forEach(func => assert.equal(func.callCount, 2));
					assert.equal(pageUpdate.callCount, 1);
					[historyBuild, historySave, historySetPage].forEach(func => assert.equal(func.callCount, 1));
					assert.equal(getPatch.callCount, 2)
					assert.equal(locked.title, newTitle);
					assert.equal(locked.content, content);
					assert.equal(locked.lock, null);
					assert.equal(locked.lockId, null);
				});
			});

			describe("and lock is not expired", function() {
				it("should throw page lock error", async function() {
					let lockId = randomInt();
					let lock = new Date();
					let pageSave = fake();
					let page = {title, content, lockId, lock, save: pageSave};
					let findOne = replace(Page, "findOne", fake(args => {
						let where = args.where;
						if (where.title === title) {
							if (where.lock || where.lockId)  {
								return null;
							} else {
								return page;
							}
						} else {
							return null;
						}
					}));
					let pageUpdate = replace(Page, "update", fake());

					await assert.rejects(
						async () => await update(title, newTitle, summary, content),
						PageLockError,
						"page is locked",
					);
					assert.equal(findOne.callCount, 2);
					[pageUpdate, pageSave].forEach(func => assert.equal(func.callCount, 1));
				});
			});
		});
	});

	afterEach(() => {
		restore();
		unwire();
	});
});

function randomInt() {
	return 1 + Math.floor(Math.random() * 2147483648);
}

const reverts = [];

function wire(mod, name, fake) {
	let obj = {[name]: mod.__get__(name)};
	let rep = replace(obj, name, fake);
	reverts.push(mod.__set__(name, rep));

	return rep;
}

function wireFunc(mod, name, func) {
	return wire(mod, name, fake(func));
}

function wireError(mod, name, Err, message) {
	return wire(mod, name, fake.throws(new Err(message)));
}

function unwire() {
	reverts.forEach(revert => revert());
	reverts.length = 0;
}
