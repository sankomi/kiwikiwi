package sanko.kiwi.dto;

import lombok.Getter;

import sanko.kiwi.domain.history.History;

@Getter
public class PageDiff {

	private String title;
	private String summary;
	private Integer event;

	private String titleDiff;
	private String contentDiff;

	private String redirect;

	public PageDiff(String title, History history, String titleDiff, String contentDiff) {
		this.title = title;
		this.summary = history.getSummary();
		this.event = history.getEvent();
		this.titleDiff = titleDiff;
		this.contentDiff = contentDiff;
	}

	public PageDiff(String redirect) {
		this.redirect = redirect;
	}

}
