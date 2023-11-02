const {Page, History} = require("./models");
const {TitleDuplicateError, PageLockError} = require("./error");
const DiffMatchPatch = require("diff-match-patch");

function index() {
	return {name: "index"};
}

async function view(title) {
	const page = await Page.findOne({where: {title}});

	if (page === null) {
		return {name: "not-exist", data: {page: {title}}};
	} else {
		return {name: "view", data: {page}};
	}
}

async function editView(title) {
	const page = await Page.findOne({where: {title}});

	if (page === null) {
		const emptyPage = Page.build({title});
		emptyPage.newTitle = title;
		return {name: "edit", data: {page: emptyPage}};
	} else {
		page.newTitle = title;
		return {name: "edit", data: {page}};
	}
}

async function editEdit(title, newTitle, summary, content) {
	let updated;
	try {
		updated = await update(title, newTitle, summary, content);
	} catch (err) {
		if (err instanceof TitleDuplicateError || err instanceof PageLockError) {
			let page = Page.build({title, summary, content});
			page.newTitle = title;
			return {name: "edit", data: {page}};
		} else {
			throw err;
		}
	}

	return {redirect: `/wiki/${updated.title}`};
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
		page = Page.build();
		page.title = newTitle;
		page.content = content;
		await page.save();

		let titlePatch = getPatch("", newTitle);
		let contentPatch = getPatch("", content);

		let history = History.build({
			summary,
			title: titlePatch,
			cntent: contentPatch,
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
		summary,
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
	return newText;
}

module.exports = {
	index,
	view,
	editView, editEdit,
}
