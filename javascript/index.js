require("dotenv").config();
const express = require("express");
const path = require("path");
const {Page, History} = require("./models");

const app = express();
const port = process.env.PORT || 3000;
app.use(express.static(path.join(__dirname, "static")));
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

app.listen(port, () => console.log(`on ${port}`));
