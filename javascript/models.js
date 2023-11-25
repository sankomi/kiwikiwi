const Sequelize = require("sequelize");
const marked = require("marked");
const JSSoup = require("jssoup").default;

const sqlite = new Sequelize(
	"database",
	"username",
	"password",
	{
		dialect: "sqlite",
		storage: process.env.DATA_PATH || "data/kiwi.db",
	},
);

const LINK_REGEX = /\[\[([^()\[\]\n\r*_`\/\\]*)\]\]/g;

const Page = sqlite.define(
	"page",
	{
		id: {
			type: Sequelize.INTEGER,
			autoIncrement: true,
			primaryKey: true,
		},
		title: {
			type: Sequelize.STRING(50),
			unique: true,
			allowNull: false,
		},
		content: {
			type: Sequelize.TEXT,
			set(value) {
				let escaped = value.replace(/&/g, "&amp;")
					.replace(/</g, "&lt;")
					.replace(/>/g, "&gt;")
					.replace(/"/g, "&quot;")
					.replace(/'/g, "&#39;");
				let linked = escaped.replace(LINK_REGEX, string => {
					let title = string.substring(2, string.length - 2);
					return string.replace(LINK_REGEX, `[${title}](/wiki/${encodeURI(title)})`);
				});
				let html = marked.parse(linked);
				let soup = new JSSoup(html);
				let text = soup.text;
				this.setDataValue("content", value);
				this.setDataValue("html", html);
				this.setDataValue("text", text);
			},
		},
		html: {type: Sequelize.TEXT},
		text: {type: Sequelize.TEXT},
		lock: {type: Sequelize.DATE},
		lockId: {
			type: Sequelize.INTEGER,
			field: "lock_id",
		},
		refresh: {
			type: Sequelize.DATE,
			defaultValue: Sequelize.NOW,
		},
	},
	{
		tableName: "pages",
		indexes: [{
			unique: true,
			fields: ["id"],
		}],
		timestamps: false,
	},
);

const History = sqlite.define(
	"history",
	{
		id: {
			type: Sequelize.INTEGER,
			autoIncrement: true,
			primaryKey: true,
		},
		event: {
			type: Sequelize.INTEGER,
			defaultValue: 1,
		},
		pageId: {
			type: Sequelize.INTEGER,
			field: "page_id",
		},
		summary: {
			type: Sequelize.STRING(100),
		},
		title: {type: Sequelize.TEXT},
		content: {type: Sequelize.TEXT},
		write: {
			type: Sequelize.DATE,
			defaultValue: Sequelize.NOW,
		},
	},
	{
		tableName: "historys",
		indexes: [{
			unique: true,
			fields: ["id"],
		}],
		timestamps: false,
	},
);

Page.hasMany(History, {foreignKey: "page_id"});
History.belongsTo(Page, {foreignKey: "page_id"});

Page.sync();
History.sync();

module.exports= {
	Page, History,
};
