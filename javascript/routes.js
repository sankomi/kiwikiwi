const express = require("express");
const path = require("path");
const router = express.Router();
router.use(express.static(path.join(__dirname, "static")));
router.use(express.urlencoded({extended: false}));

const wiki = require("./wiki");

router.get("/", (req, res) => {
	let view = wiki.index();
	render(res, view);
});

router.get("/search", async (req, res) => {
	let string = req.query.s || "";
	let current = req.query.p;

	let view = await wiki.search(string, current);
	render(res, view);
});

router.get("/wiki", async (req, res) => {
	let view = await wiki.random();
	render(res, view);
});

router.get("/wiki/:title", async (req, res) => {
	let title = req.params.title;

	let view = await wiki.view(title);
	render(res, view);
});

router.get("/edit/:title", async (req, res) => {
	let title = req.params.title;

	let view = await wiki.editView(title);
	render(res, view);
});
router.post("/edit/:title", async (req, res) => {
	let title = req.params.title;
	let newTitle = req.body.title;
	let summary = req.body.summary;
	let content = req.body.content;

	let view = await wiki.editEdit(title, newTitle, summary, content);
	render(res, view);
});

router.get("/history/:title", async (req, res) => {
	let title = req.params.title;

	let view = await wiki.history(title);
	render(res, view);
});

router.get("/history/:title/:current", async (req, res) => {
	let title = req.params.title;
	let current = req.params.current;

	let view = await wiki.history(title, current);
	render(res, view);
});

router.get("/back/:title/:event", async (req, res) => {
	let title = req.params.title;
	let event = req.params.event;

	let view = await wiki.back(title, event);
	render(res, view);
});

router.get("/diff/:title/:event", async (req, res) => {
	let title = req.params.title;
	let event = req.params.event;

	let view = await wiki.diff(title, event);
	render(res, view);
});

router.get("/rehash/:title/:event", async (req, res) => {
	let title = req.params.title;
	let event = req.params.event;

	let view = await wiki.rehash(title, event);
	render(res, view);
});

function render(res, view) {
	if (view === null) {
		res.sendStatus(404);
	} else if (view.redirect) {
		res.redirect(view.redirect);
	} else {
		res.render(view.name, view.data);
	}
}

module.exports = router;
