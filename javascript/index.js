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
		const emptyPage = Page.build({title});
		res.render("edit", {page: emptyPage});
	} else {
		res.render("edit", {page});
	}
});

app.post("/edit/:title", async (req, res) => {
	const title = req.params.title;
	const newTitle = req.body.title;
	const summary = req.body.summary;
	const content = req.body.content;

	let page = await Page.findOne({where: {title}});
	if (page === null) page = Page.build();

	if (title === newTitle) {
		page.title = title;
		page.content = content;
		page.save();
		res.redirect(`/wiki/${title}`);
	} else {
		const exist = await Page.findOne({where: {title: newTitle}});
		if (exist) {
			res.render("edit", {page: {
				title,
				newTitle,
				summary,
				content,
			}});
		} else {
			page.title = newTitle;
			page.content = content;
			page.save();
			res.redirect(`/wiki/${newTitle}`);
		}
	}
});

app.listen(port, () => console.log(`on ${port}`));
