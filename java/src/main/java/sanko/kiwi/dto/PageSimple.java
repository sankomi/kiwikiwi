package sanko.kiwi.dto;

import lombok.Getter;

import sanko.kiwi.domain.page.Page;


@Getter
public class PageSimple {

	private String title;
	private String text;

	public PageSimple(Page page) {
		this.title = page.getTitle();
		this.text = page.getContent();
	}

}
