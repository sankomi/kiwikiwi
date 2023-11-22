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

	describe("random()", function() {
		describe("if there are no pages", function() {
			it("should redirect to default page (kiwikiwi)", async function() {
				let count = replace(Page, "count", fake(() => 0));

				let view = await wiki.random();
				assert.equal(count.callCount, 1);
				assert.equal(view.redirect, "/wiki/kiwikiwi");
			});
		});

		describe("if there are pages", function() {
			let prefix = "randompage";
			let title = prefix + "title";
			let content = prefix + "content";
			let pages = [...Array(25).keys()].map(id => ({
				id,
				title: title + String(id),
				content: content + String(id),
			}));

			it("should redirect to random page", async function() {
				let count = replace(Page, "count", fake(() => pages.length));
				let findOne = replace(Page, "findOne", fake(args => pages[args.offset]));

				let view = await wiki.random();
				assert.equal(count.callCount, 1);
				assert.equal(findOne.callCount, 1);
				let equal = 0;
				pages.forEach(page => {
					if (view.redirect === `/wiki/${page.title}`) {
						equal += 1;
					}
				});
				assert.equal(equal, 1);
			});
		});
	});

	describe("search(string, current)", function() {
		describe("if search string is empty", function() {
			let emptyStrings = [null, "", "    "];
			let current = randomInt(10);
			let string = emptyStrings[randomInt(emptyStrings.length) - 1];

			it("should return search view with no pages", async function() {
				let view = await wiki.search(string, current);
				assert.equal(view.name, "search");
				assert.equal(view.data.string, "");
				assert.equal(view.data.pages, null);
			});
		})

		describe("if search has no results", function() {
			let string = "searchnoresult ";
			let current = randomInt(10);

			it("should return search view with no results", async function() {
				let findAll = replace(Page, "findAll", fake(args => []));

				let view = await wiki.search(string, current);
				assert.equal(findAll.callCount, 1);
				assert.equal(view.name, "search");
				assert.equal(view.data.string, string.trim());
				assert.equal(view.data.current, current);
				assert.equal(view.data.last, 0);
				assert.deepEqual(view.data.pages, []);
			});
		});

		describe("if search has results", function() {
			let string = "search ";
			let prefix = "search";
			let title = prefix + "title";
			let content = prefix + "content";
			let pages = [...Array(25).keys()].map(id => ({
				id,
				title: title + String(id),
				content: content + String(id),
			}));

			it("should return search view with results", async function() {
				let findAll = replace(Page, "findAll", fake(args => pages));

				let views = [null];
				for (let i = 1; i <= 3; i++) {
					views.push(await wiki.search(string, i));
				}
				assert.equal(findAll.callCount, 3);
				for (let i = 1; i <= 3; i++) {
					assert.equal(views[i].name, "search");
					assert.equal(views[i].data.string, string.trim());
					assert.equal(views[i].data.current, i);
					assert.equal(views[i].data.last, 3);
					assert.deepEqual(views[i].data.pages, pages.slice((i - 1) * 10, i * 10));
				}
			});
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
			let page = {title, content};

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
			let page = {title, content};

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

	describe("history(title, current)", function() {
		describe("if page exists", function() {
			let prefix = "historypage";
			let title = prefix + "title";
			let content = prefix + "content";
			let historyTitle = prefix + "historytitle";
			let historySummary = prefix + "historysummary";
			let historyContent = prefix + "historycontent";
			let pageTemp = {
				title, content,
				histories: [...Array(25).keys()].map(i => i + 1)
					.map(event => ({
						event,
						title: historyTitle + String(event),
						summary: historySummary + String(event),
						content: historyContent + String(event),
					})),
			};

			describe("and page number is not given", function() {
				let page = clone(pageTemp);
				it("should render history view with first 10 histories", async function() {
					let findOne = replace(Page, "findOne", fake(args => page));

					let view = await wiki.history(title);
					assert.equal(findOne.callCount, 1);
					assert.equal(view.name, "history");
					assert.equal(view.data.page.title, title);
					assert.equal(view.data.page.current, 1);
					assert.equal(view.data.page.last, 3);
					assert.equal(view.data.page.histories.length, 10);
					assert.equal(view.data.page.histories[2].title, historyTitle + "3");
				});
			});

			describe("and page number is in middle", function() {
				let current = 2;
				let page = clone(pageTemp);

				it("should render history view with middle 10 histories", async function() {
					let findOne = replace(Page, "findOne", fake(args => page));

					let view = await wiki.history(title, current);
					assert.equal(findOne.callCount, 1);
					assert.equal(view.name, "history");
					assert.equal(view.data.page.title, title);
					assert.equal(view.data.page.current, current);
					assert.equal(view.data.page.last, 3);
					assert.equal(view.data.page.histories.length, 10);
					assert.equal(view.data.page.histories[2].title, historyTitle + "13");
				});
			});

			describe("and page number is at end", function() {
				let current = 3;
				let page = clone(pageTemp);

				it("should render history view with <= 10 histories", async function() {
					let findOne = replace(Page, "findOne", fake(args => page));

					let view = await wiki.history(title, current);
					assert.equal(findOne.callCount, 1);
					assert.equal(view.name, "history");
					assert.equal(view.data.page.title, title);
					assert.equal(view.data.page.current, current);
					assert.equal(view.data.page.last, 3);
					assert.equal(view.data.page.histories.length, 5);
					assert.equal(view.data.page.histories[2].title, historyTitle + "23");
				});
			});

			describe("and page number is less than 1", function() {
				let current = -4;
				let page = clone(pageTemp);

				it("should render history view with first page", async function() {
					let findOne = replace(Page, "findOne", fake(args => page));

					let view = await wiki.history(title, current);
					assert.equal(findOne.callCount, 1);
					assert.equal(view.name, "history");
					assert.equal(view.data.page.title, title);
					assert.equal(view.data.page.current, 1);
					assert.equal(view.data.page.last, 3);
					assert.equal(view.data.page.histories.length, 10);
					assert.equal(view.data.page.histories[2].title, historyTitle + "3");
				});
			});

			describe("and page number is too big", function() {
				let current = 10;
				let page = clone(pageTemp);

				it("should render history view with last page", async function() {
					let findOne = replace(Page, "findOne", fake(args => page));

					let view = await wiki.history(title, current);
					assert.equal(findOne.callCount, 1);
					assert.equal(view.name, "history");
					assert.equal(view.data.page.title, title);
					assert.equal(view.data.page.current, 3);
					assert.equal(view.data.page.last, 3);
					assert.equal(view.data.page.histories.length, 5);
					assert.equal(view.data.page.histories[2].title, historyTitle + "23");
				});
			});
		});

		describe("if page does not exist", function() {
			let prefix = "historynopage";
			let current = randomInt();
			let title = prefix + "title";
			let content = prefix + "content";
			let historyTitle = prefix + "historytitle";
			let historySummary = prefix + "historysummary";
			let historyContent = prefix + "historycontent";

			it("should redirect to page", async function() {
				let findOne = replace(Page, "findOne", fake(args => null));

				let view = await wiki.history(title, current);
				assert.equal(findOne.callCount, 1);
				assert.deepEqual(view, {redirect: `/wiki/${title}`});
			});
		});
	});

	describe("back(title, event)", function() {
		describe("if page cannot be made", function() {
			let prefix = "backnopage";
			let event = randomInt(25);
			let title = prefix + "title";

			it("should redirect to history page", async function() {
				let make = wireFunc(wiki, "make", (title, event) => null);

				let view = await wiki.back(title, event);
				assert.equal(make.callCount, 1);
				assert.equal(view.redirect, `/wiki/history/${title}`);
			});
		});

		describe("if page can be made", function() {
			let prefix = "backpage";
			let event = randomInt(25);
			let title = prefix + "title";
			let content = prefix + "content";
			let page = {title, content};

			it("should render edit view with page", async function() {
				let make = wireFunc(wiki, "make", (title, event) => page);

				let view = await wiki.back(title, event);
				assert.equal(make.callCount, 1);
				assert.equal(view.name, "back");
				assert.deepEqual(view.data.page, {...page, event});
			});
		});
	});

	describe("diff(title, event)", function() {
		describe("if page does not exist", function() {
			let prefix = "diffnopage";
			let event = randomInt(25);
			let title = prefix + "title";

			it("should redirect to view page", async function() {
				let findOne = replace(Page, "findOne", fake(() => null));

				let view = await wiki.diff(title, event);
				assert.equal(findOne.callCount, 1);
				assert.equal(view.redirect, `/wiki/${title}`);
			});
		});

		describe("if history does not exist", function() {
			let prefix = "diffnohistory";
			let event = randomInt(25);
			let id = randomInt(50);
			let title = prefix + "title";
			let content = prefix + "content";
			let page = {id, title, content};

			it("should redirect to history page", async function() {
				let pageFindOne = replace(Page, "findOne", fake(() => page));
				let historyFindOne = replace(History, "findOne", fake(() => null));

				let view = await wiki.diff(title, event);
				assert.equal(pageFindOne.callCount, 1);
				assert.equal(historyFindOne.callCount, 1);
				assert.equal(view.redirect, `/history/${title}`);
			});
		});

		describe("if page and history exist", function() {
			let prefix = "diff";
			let event = randomInt(25);
			let id = randomInt(50);
			let title = prefix + "title";
			let content = prefix + "content";
			let page = {id, title, content};
			let historyTitle = prefix + "historytitle";
			let historySummary = prefix + "historysummary";
			let historyContent = prefix + "historycontent";
			let history = {
				title: historyTitle,
				summary: historySummary,
				content: historyContent,
			};
			let titleDiff = prefix + "titlediff";
			let contentDiff = prefix + "contentdiff";

			it("should return title and content diffs", async function() {
				let pageFindOne = replace(Page, "findOne", fake(() => page));
				let historyFindOne = replace(History, "findOne", fake(args => {
					let where = args.where;
					if (where.event === event && where.pageId === id) {
						return history;
					} else {
						return null;
					}
				}));
				let formatDiff = wireFunc(wiki, "formatDiff", diff => {
					switch(diff) {
						case historyTitle:
							return titleDiff;
						case historyContent:
							return contentDiff;
					}
				});

				let view = await wiki.diff(title, event);
				assert.equal(pageFindOne.callCount, 1);
				assert.equal(historyFindOne.callCount, 1);
				assert.equal(formatDiff.callCount, 2);
				assert.equal(view.name, "diff");
				assert.equal(view.data.page.title, title);
				assert.equal(view.data.page.event, event);
				assert.equal(view.data.page.titleDiff, titleDiff);
				assert.equal(view.data.page.contentDiff, contentDiff);
			});
		});
	});

	describe("rehash(title, event)", function() {
		describe("if page cannot be made", function() {
			let prefix = "rehashnopage";
			let event = randomInt(50);
			let title = prefix + "title";

			it("should redirect to view page", async function() {
				let make = wireFunc(wiki, "make", (title, event) => null);

				let view = await wiki.rehash(title, event);
				assert.equal(make.callCount, 1);
				assert.equal(view.redirect, `/wiki/${title}`);
			});
		});

		describe("if page can be made", function() {
			describe("and page cannot be updated", function() {
				let prefix = "rehashnoupdate";
				let event = randomInt(50);
				let title = prefix + "title";
				let newTitle = prefix + "newtitle";
				let content = prefix + "content";
				let back = {newTitle, content};

				it("should redirect to back page", async function() {
					let make = wireFunc(wiki, "make", (title, event) => back);
					let update = randomInt(2) > 1?
						wireError(wiki, "update", PageLockError, "page is locked"):
						wireError(wiki, "update", TitleDuplicateError, "page with new title already exists");

					let view = await wiki.rehash(title, event);
					assert.equal(make.callCount, 1);
					assert.equal(update.callCount, 1);
					assert.equal(view.redirect, `/back/${title}/${event}`);
				});
			});

			describe("and page can be updated", function() {
				let prefix = "rehash";
				let event = randomInt(50);
				let title = prefix + "title";
				let newTitle = prefix + "newtitle";
				let summary = prefix + "summary";
				let content = prefix + "content";
				let back = {title: newTitle, summary, content};

				it("should redirect to new view page", async function() {
					let make = wireFunc(wiki, "make", (title, event) => back);
					let update = wireFunc(wiki, "update", args => back);

					let view = await wiki.rehash(title, event);
					assert.equal(make.callCount, 1);
					assert.equal(update.callCount, 1);
					assert.deepEqual(update.getCall(0).args, [title, newTitle, `rehash(${event})`, content]);
					assert.equal(view.redirect, `/wiki/${newTitle}`);
				});
			});
		});
	});

	describe("make(title, event)", function() {
		let make = wiki.__get__("make");

		describe("if page does not exist", function() {
			let prefix = "makenopage";
			let event = randomInt(25);
			let title = prefix + "title";

			it("should return null", async function() {
				let findOne = replace(Page, "findOne", fake(() => null));

				let back = await make(title, event);
				assert.equal(findOne.callCount, 1);
				assert.equal(back, null);
			});
		});

		describe("if history does not exist", function() {
			let prefix = "makenohistory";
			let event = randomInt(25);
			let title = prefix + "title";
			let content = prefix + "content";
			let page = {title, content};

			it("should return null", async function() {
				let pageFindOne = replace(Page, "findOne", fake(() => page));
				let historyFindOne = replace(History, "findOne", fake(() => null));

				let back = await make(title, event);
				assert.equal(pageFindOne.callCount, 1);
				assert.equal(historyFindOne.callCount, 1);
				assert.equal(back, null);
			});
		});

		describe("if page and histories exist", function() {
			let prefix = "make";
			let id = randomInt(50);
			let event = 15;
			let title = prefix + "title";
			let content = prefix + "content";
			let historyTitle = prefix + "historytitle";
			let historySummary = prefix + "historysummary";
			let historyContent = prefix + "historycontent";

			it("should return old page", async function() {
				let page = {
					id, title, content,
					histories: [...Array(25).keys()].map(i => i + 1)
						.map(event => ({
							event,
							title: historyTitle + String(event),
							summary: historySummary + String(event),
							content: historyContent + String(event),
						})),
				};
				let pageFindOne = replace(Page, "findOne", fake(() => page));
				let historyFindOne = replace(History, "findOne", fake(args => {
					if (args.where.pageId !== page.id) return null;
					let event = args.where.event;
					return {
						title: historyTitle + String(event),
						summary: historySummary + String(event),
						content: historyContent + String(event),
						event: args.where.event,
					};
				}));
				let applyPatch = wireFunc(wiki, "applyPatch", (text1, text2) => text2 + "patched");
				let pageBuild = replace(Page, "build", fake(args => ({...args})));

				let back = await make(title, event);
				[pageFindOne, historyFindOne, pageBuild].forEach(func => assert.equal(func.callCount, 1));
				assert.equal(applyPatch.callCount, 2 * event);
				assert.equal(back.title, historyTitle + String(event) + "patched");
				assert.equal(back.content, historyContent + String(event) + "patched");
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

function randomInt(max = 2147483648) {
	return 1 + Math.floor(Math.random() * max);
}

function clone(object) {
	return JSON.parse(JSON.stringify(object));
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
