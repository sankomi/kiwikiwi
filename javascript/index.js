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

app.listen(port, () => console.log(`on ${port}`));
