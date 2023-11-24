require("dotenv").config();
const express = require("express");
const app = express();
const port = process.env.PORT || 3000;
app.set("view engine", "ejs");

const routes = require("./routes");
app.use("/", routes);

app.listen(port, () => console.log(`on ${port}`));
