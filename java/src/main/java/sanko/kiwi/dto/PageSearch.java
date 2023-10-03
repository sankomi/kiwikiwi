package sanko.kiwi.dto;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

import sanko.kiwi.domain.page.Page;

@Getter
public class PageSearch {

	private String search;

	private int current;
	private int last;

	private List<PageSimple> pages;

	public PageSearch(String search, int current, int last, List<Page> pages) {
		this.search = search;
		this.current = current;
		this.last = last;
		if (pages == null) {
			this.pages = null;
		} else {
			this.pages = pages.stream()
				.map(PageSimple::new)
				.collect(Collectors.toList());
		}
	}

}
