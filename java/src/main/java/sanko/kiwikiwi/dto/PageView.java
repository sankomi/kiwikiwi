package sanko.kiwikiwi.dto;

import lombok.Getter;

import sanko.kiwikiwi.domain.page.Page;

@Getter
public class PageView {

	private String title;
	private String html;

	public PageView(Page page) {
		this.title = page.getTitle();
		this.html = page.getHtml();
	}

}
