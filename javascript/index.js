require("dotenv").config();
const express = require("express");
const path = require("path");
const {Page, History} = require("./models");
const DiffMatchPatch = require("diff-match-patch");

const app = express();
const port = process.env.PORT || 3000;
app.use(express.static(path.join(__dirname, "static")));
app.use(express.urlencoded({extended: false}));
app.set("view engine", "ejs");

app.get("/", (req, res) => {
	res.render("index");
});

app.get("/wiki/:title", async (req, res) => {
	const title = req.params.title;
	const page = await Page.findOne({where: {title}});

	if (page === null) {
		res.render("not-exist", {page: {title}});
	} else {
		res.render("view", {page});
	}
});

app.get("/edit/:title", async (req, res) => {
	const title = req.params.title;
	const page = await Page.findOne({where: {title}});

	if (page === null) {
		const emptyPage = Page.build({title});
		emptyPage.newTitle = title;
		res.render("edit", {page: emptyPage});
	} else {
		page.newTitle = title;
		res.render("edit", {page});
	}
});

app.post("/edit/:title", async (req, res) => {
	const title = req.params.title;
	const newTitle = req.body.title;
	const summary = req.body.summary;
	const content = req.body.content;

	let updated;
	try {
		updated = await update(title, newTitle, content, summary);
	} catch (err) {
		let page = Page.build();
		page.title = title;
		page.newTitle = title;
		page.content = content;
		return res.render("edit", {page});
	}

	res.redirect(`/wiki/${updated.title}`);
});

async function update(title, newTitle, content, summary) {
	let page = await Page.findOne({where: {title}});

	if (title !== newTitle) {
		let newPage = await Page.findOne({where: {title: newTitle}});

		if (newPage !== null) {
			throw new Error("page with new title already exists");
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
		throw new Error("page is locked");
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

app.listen(port, () => console.log(`on ${port}`));
