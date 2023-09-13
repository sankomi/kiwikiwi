package sanko.kiwikiwi.dto;

import lombok.Getter;

import sanko.kiwikiwi.domain.page.Page;

@Getter
public class PageEdit {

	private String title;
	private String newTitle;
	private String content;
	private String html;

	private String redirect;

	public PageEdit(Page page, String title) {
		this.title = title;
		this.newTitle = page.getTitle();
		this.content = page.getContent();
		this.html = page.getHtml();
		this.redirect = null;
	}

	public PageEdit(Page page) {
		this(page, page.getTitle());
	}

	public PageEdit(String redirect) {
		this.redirect = redirect;
	}

}
