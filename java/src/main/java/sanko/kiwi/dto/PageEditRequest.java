package sanko.kiwi.dto;

import lombok.*; //Getter, Builder

@Getter
public class PageEditRequest {

	private String title;
	private String content;
	private String summary;

	@Builder
	public PageEditRequest(String title, String content, String summary) {
		this.title = title;
		this.content = content;
		this.summary = summary;
	}

}
