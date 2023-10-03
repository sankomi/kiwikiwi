package sanko.kiwi.dto;

import lombok.Getter;

import sanko.kiwi.domain.page.Page;

@Getter
public class PageEdit {

	private String title;
	private String newTitle;
	private String summary;
	private String content;
	private String html;

	private String redirect;

	public PageEdit(Page page, String newTitle, String summary) {
		this.title = page.getTitle();
		this.newTitle = newTitle;
		this.summary = summary;
		this.content = page.getContent();
		this.html = page.getHtml();
		this.redirect = null;
	}

	public PageEdit(Page page) {
		this(page, page.getTitle(), "");
	}

	public PageEdit(String redirect) {
		this.redirect = redirect;
	}

}
