class TitleDuplicateError extends Error {
	constructor(message) {
		super(message);
		this.name = this.constructor.name;
	}
}

class PageLockError extends Error {
	constructor(message) {
		super(message);
		this.name = this.constructor.name;
	}
}

module.exports = {TitleDuplicateError, PageLockError};
