package sanko.kiwi.dto;

import lombok.Getter;

import sanko.kiwi.domain.page.Page;

@Getter
public class PageBack {

	private String title;
	private String html;
	private Integer event;

	private String redirect;

	public PageBack(Page page, Integer event) {
		this.title = page.getTitle();
		this.html = page.getHtml();
		this.event = event;
	}

	public PageBack(String redirect) {
		this.redirect = redirect;
	}

}
