const {Page, History} = require("./db");

Promise.all([Page.findAll(), History.findAll()])
	.then(console.log);
