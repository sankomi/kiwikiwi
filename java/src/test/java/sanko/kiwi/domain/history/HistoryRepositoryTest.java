package sanko.kiwi.domain.history;

import java.util.Optional;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import sanko.kiwi.domain.page.*; //Page, PageRepository

@DataJpaTest
class HistoryRepositoryTest {

	@Autowired
	private HistoryRepository historyRepository;

	@Autowired
	private PageRepository pageRepository;

	private Page createPage(String title, String content) {
		Page page = Page.builder()
			.title(title)
			.content(content)
			.build();
		return pageRepository.save(page);
	}

	@Test
	void testHistorySave() {
		//given
		String pageTitle = "savepagetitle";
		String pageContent = "savepagecontent";
		Page page = createPage(pageTitle, pageContent);

		Integer event = 1;
		String summary = "savesummary";
		String title = "savetitle";
		String content = "savecontent";
		History history = History.builder()
			.page(page)
			.event(event)
			.summary(summary)
			.title(title)
			.content(content)
			.build();

		//when
		historyRepository.save(history);

		//then
		assertTrue(history.getId() > 0L);
		assertEquals(event, history.getEvent());
		assertEquals(summary, history.getSummary());

		assertEquals(pageTitle, history.getPage().getTitle());
		assertEquals(pageContent, history.getPage().getContent());
	}

	@Test
	void testHistoryFind() {
		//given
		String pageTitle = "findpagetitle";
		String pageContent = "findpagecontent";
		Page page = createPage(pageTitle, pageContent);

		Integer event = 1;
		String summary = "findsummary";
		String title = "findtitle";
		String content = "findcontent";
		History history = History.builder()
			.page(page)
			.event(event)
			.summary(summary)
			.title(title)
			.content(content)
			.build();
		historyRepository.save(history);
		Long id = history.getId();

		//when
		Optional<History> found = historyRepository.findById(id);

		//then
		assertTrue(found.isPresent());
		found.ifPresent(h -> {
			assertEquals(id, h.getId());
			assertEquals(event, h.getEvent());
			assertEquals(summary, h.getSummary());

			assertEquals(pageTitle, h.getPage().getTitle());
			assertEquals(pageContent, h.getPage().getContent());
		});
	}

}
