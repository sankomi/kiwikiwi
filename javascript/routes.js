const express = require("express");
const path = require("path");
const router = express.Router();
router.use(express.static(path.join(__dirname, "static")));
router.use(express.urlencoded({extended: false}));

const wiki = require("./wiki");

router.get("/", wiki.index);

router.get("/wiki/:title", wiki.view);

router.get("/edit/:title", wiki.editView);
router.post("/edit/:title", wiki.editEdit);

module.exports = router;
