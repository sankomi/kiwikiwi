require("dotenv").config();
const express = require("express");
const path = require("path");
const {Page, History} = require("./models");

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
		const emptyPage = Page.build({title, newTitle: title});
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
		page.save();

		return page;
	}

	page.title = newTitle;
	page.content = content;
	page.save();

	return page;
}

app.listen(port, () => console.log(`on ${port}`));
