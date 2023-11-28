const {Op} = require("sequelize");

const {Page, History} = require("./models");
const {TitleDuplicateError, PageLockError} = require("./error");
const DiffMatchPatch = require("diff-match-patch");

const TITLE_REGEX = /[\(\)\[\]\n\r*_`/\\]/g;

function index() {
	return {name: "index"};
}

async function random() {
	let count = await Page.count();

	if (count > 0) {
		let index = Math.floor(Math.random() * count);
		let page = await Page.findOne({offset: index});
		return {redirect: `/wiki/${page.title}`};
	} else {
		return {redirect: "/wiki/kiwikiwi"};
	}
}

async function search(string, current) {
	if (!string || string.trim() === "") {
		return {name: "search", data: {string: "", pages: null}};
	}
	string = string.trim();

	current = +current;
	if (!current || current <= 0) {
		current = 1;
	}

	let pages = await Page.findAll({
		where: {
			[Op.or]: [
				{title: {[Op.like]: `%${string}%`}},
				{text: {[Op.like]: `%${string}%`}},
			],
		},
	});
	let last = Math.ceil(pages.length / 10);
	let show = pages.slice((current - 1) * 10, current * 10);
	return {name: "search", data: {string, current, last, pages: show}};
}

async function view(title) {
	const page = await Page.findOne({where: {title}});

	if (page === null) {
		if (title.match(TITLE_REGEX)) {
			return null;
		}
		return {name: "not-exist", data: {page: {title}}};
	} else {
		return {name: "view", data: {page}};
	}
}

async function editView(title) {
	const page = await Page.findOne({where: {title}});

	if (page === null) {
		if (title.match(TITLE_REGEX)) {
			return null;
		}
		const emptyPage = Page.build({title});
		emptyPage.newTitle = title;
		return {name: "edit", data: {page: emptyPage}};
	} else {
		page.newTitle = title;
		return {name: "edit", data: {page}};
	}
}

async function editEdit(title, newTitle, summary, content) {
	if (newTitle.match(TITLE_REGEX)) {
		newTitle = newTitle.replace(TITLE_REGEX, "");
		let page = Page.build({title, content});
		page.newTitle = newTitle;
		page.summary = summary;
		return {name: "edit", data: {page}};
	}

	let updated;
	try {
		updated = await update(title, newTitle, summary, content);
	} catch (err) {
		if (err instanceof TitleDuplicateError || err instanceof PageLockError) {
			let page = Page.build({title, content});
			page.newTitle = title;
			return {name: "edit", data: {page}};
		} else {
			throw err;
		}
	}

	return {redirect: `/wiki/${updated.title}`};
}

async function history(title, current = 1) {
	current = +current;
	if (!current) {
		current = 1;
	}

	let page = await Page.findOne({
		where: {title},
		include: [History],
		order: [[History, "event", "DESC"]],
	});

	if (page === null) {
		if (title.match(TITLE_REGEX)) {
			return null;
		}
		return {redirect: `/wiki/${title}`};
	} else {
		last = Math.ceil(page.histories.length / 10);
		if (current < 1) current = 1;
		else if (current > last) current = last;

		page.histories = page.histories.slice((current - 1) * 10, current * 10);
		page.current = current;
		page.last = last;
		return {name: "history", data: {page}};
	}
}

async function back(title, event) {
	let back = await make(title, event);

	if (back === null) {
		if (title.match(TITLE_REGEX)) {
			return null;
		}
		return {redirect: `/wiki/history/${title}`};
	} else {
		back.event = event;
		return {name: "back", data: {page: back}};
	}
}

async function diff(title, event) {
	let page = await Page.findOne({where: {title}});

	if (page === null) {
		if (title.match(TITLE_REGEX)) {
			return null;
		}
		return {redirect: `/wiki/${title}`};
	}

	let history = await History.findOne({where: {event, pageId: page.id}});

	if (history === null) {
		return {redirect: `/history/${title}`};
	}

	let titleDiff = formatDiff(history.title || "");
	let contentDiff = formatDiff(history.content || "");

	page.event = event;
	page.titleDiff = titleDiff;
	page.contentDiff = contentDiff;
	return {name: "diff", data: {page}};
}

function formatDiff(diff) {
	diff = diff.replace(/\n\+([^\n]*)/g, "\n##ins##+$1##/ins##");
	diff = diff.replace(/\n\-([^\n]*)/g, "\n##del##-$1##/del##");
	diff = diff.replace(/@@\s\-\d+,{0,1}\d*\s\+\d+,{0,1}\d*\s@@\n{0,1}/g, "");
	diff = diff.replace(/(%0D)*%0A/g, "%0A ");
	diff = decodeURI(diff);
	diff = diff.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&#39;");
	return diff.replace(/##(ins|\/ins|del|\/del)##/g, "<$1>");
}

async function rehash(title, event) {
	let back = await make(title, event);

	if (back === null) {
		if (title.match(TITLE_REGEX)) {
			return null;
		}
		return {redirect: `/wiki/${title}`};
	}

	try {
		updated = await update(title, back.title, `rehash(${event})`, back.content);
	} catch (err) {
		if (err instanceof TitleDuplicateError || err instanceof PageLockError) {
			return {redirect: `/back/${title}/${event}`};
		} else {
			throw err;
		}
	}

	return {redirect: `/wiki/${updated.title}`};
}

async function make(title, event) {
	let page = await Page.findOne({
		where: {title},
		include: [History],
		order: [[History, "event", "ASC"]],
	});

	if (page === null) {
		return null;
	}

	let history = await History.findOne({where: {event, pageId: page.id}});

	if (history === null) {
		return null;
	}

	backTitle = "";
	backContent = "";

	for (let history of page.histories) {
		if (history.event > event) break;
		backTitle = applyPatch(backTitle, history.title);
		backContent = applyPatch(backContent, history.content);
	}

	return Page.build({title: backTitle, content: backContent});
}

async function update(title, newTitle, summary, content) {
	let page = await Page.findOne({where: {title}});

	if (title !== newTitle) {
		let newPage = await Page.findOne({where: {title: newTitle}});

		if (newPage !== null) {
			throw new TitleDuplicateError("page with new title already exists");
		}
	}

	if (page === null) {
		page = Page.build({title: newTitle, content});
		await page.save();

		let titlePatch = getPatch("", newTitle);
		let contentPatch = getPatch("", content);

		let history = History.build({
			summary: summary || "create",
			title: titlePatch,
			content: contentPatch,
		});
		await history.save()
			.then(() => history.setPage(page));

		return page;
	}

	if (page.lock !== null && page.lock <= new Date()) {
		page.lockId = null;
		page.lock = null;
		await page.save();
	}

	let id = page.id;
	let lockId = Math.floor(Math.random() * 2147483647);
	let lock = new Date(+new Date() + 60000);

	await Page.update(
		{lockId, lock},
		{where: {
			id,
			lockId: null,
		}},
	);

	let locked = await Page.findOne({
		where :{
			title,
			lock,
			lockId,
		},
		include: [History],
		order: [[History, "event", "DESC"]],
	});

	if (locked === null) {
		throw new PageLockError("page is locked");
	}

	let titlePatch = getPatch(locked.title, newTitle);
	let contentPatch = getPatch(locked.content, content);
	let event = locked.histories[0].event + 1;
	let history = History.build({
		summary: summary || "edit",
		event,
		title: titlePatch,
		content: contentPatch,
	});
	await history.save()
		.then(() => history.setPage(locked));

	locked.title = newTitle;
	locked.content = content;
	locked.lockId = null;
	locked.lock = null;
	locked.refresh = new Date();
	await locked.save();

	return locked;
}

function getPatch(text1, text2) {
	let dmp = new DiffMatchPatch();
	let diff = dmp.diff_main(text1, text2, false);
	dmp.diff_cleanupSemantic(diff);
	let patch = dmp.patch_make(diff);
	return dmp.patch_toText(patch);
}

function applyPatch(text, patchText) {
	let dmp = new DiffMatchPatch();
	let patch = dmp.patch_fromText(patchText);
	let newText = dmp.patch_apply(patch, text);
	return newText[0];
}

module.exports = {
	index,
	random,
	search,
	view,
	editView, editEdit,
	history, back, diff, rehash,
}
