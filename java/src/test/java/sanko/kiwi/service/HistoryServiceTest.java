package sanko.kiwi.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*; //when, verify, times
import static org.junit.jupiter.api.Assertions.assertEquals;

import sanko.kiwi.domain.page.*; //Page, PageRepository
import sanko.kiwi.domain.history.*; //History, HistoryRepository

@ExtendWith(SpringExtension.class)
@Import(HistoryService.class)
class HistoryServiceTest {

	@Autowired
	private HistoryService historyService;

	@MockBean
	private PageRepository pageRepository;

	@MockBean
	private HistoryRepository historyRepository;

	@Test
	void testHistorySave() {
		//given
		String prefix = "save";
		String title = prefix + "title";
		String summary = prefix + "summary";
		String content = prefix + "content";
		Integer event = 5;
		Page page = new Page(title, content);
		History history = History.builder()
			.page(page)
			.event(event)
			.summary(summary)
			.title(title)
			.content(content)
			.build();
		when(historyRepository.findFirstByPageOrderByEventDesc(any(Page.class)))
			.thenReturn(history);

		//when
		String newTitle = prefix + "newtitle";
		String newSummary = prefix + "newsummary";
		String newContent = prefix + "newcontent";
		historyService.save(page, newTitle, newSummary, newContent);

		//then
		ArgumentCaptor<History> argument = ArgumentCaptor.forClass(History.class);
		verify(historyRepository, times(1)).save(argument.capture());
		assertEquals(event + 1, argument.getValue().getEvent());
		assertEquals(newSummary, argument.getValue().getSummary());
	}

	@Test
	void testHistorySaveNoHistory() {
		//given
		String prefix = "save";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = new Page(title, content);
		when(historyRepository.findFirstByPageOrderByEventDesc(any(Page.class)))
			.thenReturn(null);

		//when
		String newTitle = prefix + "newtitle";
		String newSummary = prefix + "newsummary";
		String newContent = prefix + "newcontent";
		historyService.save(page, newTitle, newSummary, newContent);

		//then
		ArgumentCaptor<History> argument = ArgumentCaptor.forClass(History.class);
		verify(historyRepository, times(1)).save(argument.capture());
		assertEquals(1, argument.getValue().getEvent());
		assertEquals(newSummary, argument.getValue().getSummary());
	}

}
