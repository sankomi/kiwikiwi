package sanko.kiwi.dto;

import lombok.Getter;

import sanko.kiwi.domain.page.Page;

@Getter
public class PageRehash {

	private String redirect;

	public PageRehash(String redirect) {
		this.redirect = redirect;
	}

}
