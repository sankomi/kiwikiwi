package sanko.kiwi.domain.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import sanko.kiwi.domain.page.Page;

class HistoryTest {

	@Test
	void testHistoryBuilder() {
		//given
		String pageTitle = "pagetitle";
		String pageContent = "pagecontent";
		Page page = new Page(pageTitle, pageContent);

		Integer event = 1;
		String summary = "summary";
		String title = "title";
		String content = "content";

		//when
		History history = History.builder()
			.page(page)
			.event(event)
			.summary(summary)
			.title(title)
			.content(content)
			.build();

		//then
		assertEquals(event, history.getEvent());
		assertEquals(summary, history.getSummary());

		assertEquals(pageTitle, history.getPage().getTitle());
		assertEquals(pageContent, history.getPage().getContent());
	}

}
