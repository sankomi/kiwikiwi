package sanko.kiwikiwi.domain.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoryTest {

	@Test
	void testHistoryBuilder() {
		//given
		Integer event = 1;
		String summary = "summary";
		String title = "title";
		String content = "content";

		//when
		History history = History.builder()
			.event(event)
			.summary(summary)
			.title(title)
			.content(content)
			.build();

		//then
		assertEquals(event, history.getEvent());
		assertEquals(summary, history.getSummary());
		assertEquals(title, history.getTitle());
		assertEquals(content, history.getContent());
	}

}
