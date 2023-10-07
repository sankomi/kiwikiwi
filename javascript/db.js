const Sequelize = require("sequelize");
const sequelize = new Sequelize(
	"database",
	"username",
	"password",
	{
		dialect: "sqlite",
		storage: "data/kiwi.db",
	},
);

const Page = sequelize.define(
	"Page",
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
		content: {type: Sequelize.TEXT},
		html: {type: Sequelize.TEXT},
		text: {type: Sequelize.TEXT},
		lock: {type: Sequelize.DATE},
		lock_id: {type: Sequelize.INTEGER},
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

const History = sequelize.define(
	"History",
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
		page_id: {
			type: Sequelize.INTEGER,
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

module.exports= {
	Page, History,
};
