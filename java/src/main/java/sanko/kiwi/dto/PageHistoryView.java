package sanko.kiwi.dto;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

import sanko.kiwi.domain.page.Page;
import sanko.kiwi.domain.history.History;

@Getter
public class PageHistoryView {

	private String title;

	private int current;
	private int last;

	private List<PageHistory> historys;

	private String redirect;

	public PageHistoryView(Page page, int current, int last, List<History> historys) {
		this.title = page.getTitle();
		this.current = current;
		this.last = last;
		this.historys = historys.stream()
			.map(PageHistory::new)
			.collect(Collectors.toList());
	}

	public PageHistoryView(String redirect) {
		this.redirect = redirect;
	}

}
