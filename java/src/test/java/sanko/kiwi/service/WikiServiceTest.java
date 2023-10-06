package sanko.kiwi.service;

import java.util.*; //List, Arrays, ArrayList
import java.time.LocalDateTime;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.*; //when, doAnswer, verify, times
import static org.junit.jupiter.api.Assertions.*; //assertEquals, assertTrue, assertThrows
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import sanko.kiwi.domain.page.Page;
import sanko.kiwi.domain.history.History;
import sanko.kiwi.dto.*; //PageView, PageEditRequest, PageEdit, PageHistoryView, PageBack, PageRehash, PageDiff

@ExtendWith(SpringExtension.class)
@Import(WikiService.class)
class WikiServiceTest {

	@Autowired
	private WikiService wikiService;

	@MockBean
	private PageService pageService;

	@MockBean
	private HistoryService historyService;

	private static Long pageId = 0L;

	private Page createPage(String title, String content) {
		Page page = new Page(title, content);
		setField(page, "id", ++pageId);
		when(pageService.find(title))
			.thenReturn(page);
		return page;
	}

	private void updatePage(Page page, String title, String content, Integer event) {
		History history = createHistory(page, title, content, event);
		page.update(title, content);
		List<History> historys = page.getHistorys();
		if (historys == null) {
			historys = new ArrayList<>();
		}
		historys.add(history);
		setField(page, "historys", historys);
		when(pageService.find(title))
			.thenReturn(page);
	}

	private History createHistory(Page page, String title, String content, Integer event) {
		History history = History.builder()
			.page(page)
			.event(event)
			.summary("summary" + String.valueOf(event))
			.title(title)
			.content(content)
			.build();
		when(historyService.find(any(Page.class), eq(event)))
			.thenReturn(history);
		return history;
	}

	private List<History> createHistorys(Page page, int number) {
		List<History> historys = new ArrayList();
		for (int i = 0; i < number; i++) {
			History history = History.builder()
				.page(page)
				.event(i)
				.summary("summary" + String.valueOf(i))
				.title("title" + String.valueOf(i))
				.content("content" + String.valueOf(i))
				.build();
			historys.add(history);
			when(historyService.find(any(Page.class), eq(i)))
				.thenReturn(history);
		}
		setField(page, "historys", historys);
		return historys;
	}

	@Test
	void testWikiPageView() {
		//given
		String title = "viewtitle";
		String heading = "viewheading";
		String paragraph = "viewparagraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		Page page = createPage(title, content);

		//when
		PageView pageView = wikiService.view(title);

		//then
		assertEquals(title, pageView.getTitle());
		assertTrue(pageView.getHtml().contains(heading));
		assertTrue(pageView.getHtml().contains(paragraph));
	}

	@Test
	void testWikiPageNotFound() {
		//given
		String prefix = "notfound";
		String title = prefix + "title";

		when(pageService.find(title))
			.thenReturn(null);

		//when
		PageView pageView = wikiService.view(title);

		//then
		assertEquals(null, pageView);
	}

	@Test
	void testWikiRandomPage() {
		//given
		String prefix = "randompage";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);

		when(pageService.getRandomPage())
			.thenReturn(page);

		//when
		String random = wikiService.getRandomPage();

		//then
		assertEquals(title, random);
		verify(pageService, times(1)).getRandomPage();
	}

	@Test
	void testWikiRandomPageNotFound() {
		//given
		String prefix = "randompagenotfound";
		String title = prefix + "title";

		when(pageService.getRandomPage())
			.thenReturn(null);

		//when
		String random = wikiService.getRandomPage();

		//then
		assertEquals("kiwikiwi", random);
		verify(pageService, times(1)).getRandomPage();
	}

	@Test
	void testWikiPageInvalidTitle() {
		List<String> invalids = Arrays.asList("(", ")", "[", "]", "\r", "\n", "*", "_", "`", "/", "\\");

		for (String s : invalids) {
			//given
			String title = String.format("invalid%stitle", s);

			when(pageService.find(title))
				.thenReturn(null);

			//whenthen
			ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> wikiService.view(title)
			);
			assertTrue(exception.getMessage().contains("404"));
		}
	}

	@Test
	void testWikiPageViewEdit() {
		//given
		String title = "viewedittitle";
		String heading = "vieweditheading";
		String paragraph = "vieweditparagraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		Page page = createPage(title, content);

		//when
		PageEdit pageEdit = wikiService.viewEdit(title);

		//then
		assertEquals(title, pageEdit.getTitle());
		assertEquals(title, pageEdit.getNewTitle());
		assertEquals(content, pageEdit.getContent());
		assertTrue(pageEdit.getHtml().contains(heading));
		assertTrue(pageEdit.getHtml().contains(paragraph));
	}

	@Test
	void testWikiPageEditNoPage() {
		//given
		String prefix = "editnopage";
		String title = prefix + "title";
		String newTitle = title;
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		String summary = prefix + "summary";
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary(summary)
			.build();

		when(pageService.find(title))
			.thenReturn(null);
		when(pageService.create())
			.thenReturn(Page.builder()
				.title("")
				.content("")
				.build()
			);
		doAnswer(invocation -> {
			Page page = (Page) invocation.getArguments()[0];
			page.update(title, content);
			return null;
		})
			.when(pageService)
			.update(any(Page.class), eq(title), eq(content));

		//when
		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals("/wiki/" + title, pageEdit.getRedirect());
	}

	@Test
	void testWikiPageEditNoPageNoSummary() {
		//given
		String prefix = "editnopagenosummary";
		String title = prefix + "title";
		String newTitle = title;
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary("")
			.build();

		when(pageService.find(title))
			.thenReturn(null);
		when(pageService.create())
			.thenReturn(Page.builder()
				.title("")
				.content("")
				.build()
			);
		doAnswer(invocation -> {
			Page page = (Page) invocation.getArguments()[0];
			page.update(title, content);
			return null;
		})
			.when(pageService)
			.update(any(Page.class), eq(title), eq(content));

		//when
		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals("/wiki/" + title, pageEdit.getRedirect());
		verify(historyService).save(any(Page.class), eq(title), eq("create"), eq(content));
	}

	@Test
	void testWikiPageEditNoPageButTitleExists() {
		//given
		String prefix = "editnopagebuttitleexists";
		String title = prefix + "title";
		String newTitle = prefix + "newtitle";
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		String summary = prefix + "summary";
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary(summary)
			.build();

		when(pageService.find(title))
			.thenReturn(null);
		createPage(newTitle, content);
		when(pageService.create(title, content))
			.thenReturn(Page.builder()
				.title(title)
				.content(content)
				.build()
			);

		//when
		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals(title, pageEdit.getTitle());
		assertEquals(newTitle, pageEdit.getNewTitle());
		assertEquals(content, pageEdit.getContent());
		assertTrue(pageEdit.getHtml().contains(heading));
		assertTrue(pageEdit.getHtml().contains(paragraph));
		assertEquals(null, pageEdit.getRedirect());
	}

	@Test
	void testWikiPageEditPageSameTitle() {
		//given
		String prefix = "editpagesametitle";
		String title = prefix + "title";
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);

		Page page = createPage(title, content);
		when(pageService.checkLock(title))
			.thenReturn(false);
		when(pageService.checkLock(eq(title), any(LocalDateTime.class), any(Integer.class)))
			.thenReturn(false);
		doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			Page mockPage = (Page) args[0];
			String mockTitle = (String) args[1];
			String mockContent = (String) args[2];
			mockPage.update(mockTitle, mockContent);
			return null;
		})
			.when(pageService)
			.update(any(Page.class), any(String.class), any(String.class));

		//when
		String newHeading = prefix + "newheading";
		String newParagraph = prefix + "newparagraph";
		String newContent = String.format("# %s\n\n%s", newHeading, newParagraph);
		String newSummary = prefix + "newsummary";
		PageEditRequest request = PageEditRequest.builder()
			.title(title)
			.content(newContent)
			.summary(newSummary)
			.build();

		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals(title, page.getTitle());
		assertEquals(newContent, page.getContent());
		assertTrue(page.getHtml().contains(newHeading));
		assertTrue(page.getHtml().contains(newParagraph));
		assertEquals("/wiki/" + title, pageEdit.getRedirect());
	}

	@Test
	void testWikiPageEditPageSameTitleNoSummary() {
		//given
		String prefix = "editpagesametitlenosummary";
		String title = prefix + "title";
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);

		Page page = createPage(title, content);
		when(pageService.checkLock(title))
			.thenReturn(false);
		when(pageService.checkLock(eq(title), any(LocalDateTime.class), any(Integer.class)))
			.thenReturn(false);
		doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			Page mockPage = (Page) args[0];
			String mockTitle = (String) args[1];
			String mockContent = (String) args[2];
			mockPage.update(mockTitle, mockContent);
			return null;
		})
			.when(pageService)
			.update(any(Page.class), any(String.class), any(String.class));

		//when
		String newHeading = prefix + "newheading";
		String newParagraph = prefix + "newparagraph";
		String newContent = String.format("# %s\n\n%s", newHeading, newParagraph);
		PageEditRequest request = PageEditRequest.builder()
			.title(title)
			.content(newContent)
			.summary("")
			.build();

		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals(title, page.getTitle());
		assertEquals(newContent, page.getContent());
		assertTrue(page.getHtml().contains(newHeading));
		assertTrue(page.getHtml().contains(newParagraph));
		assertEquals("/wiki/" + title, pageEdit.getRedirect());
		verify(historyService).save(any(Page.class), eq(title), eq("edit"), eq(newContent));
	}

	@Test
	void testWikiPageEditPageNewTitle() {
		//given
		String prefix = "editpagenewtitle";
		String title = prefix + "title";
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);

		Page page = createPage(title, content);
		when(pageService.checkLock(title))
			.thenReturn(false);
		when(pageService.checkLock(eq(title), any(LocalDateTime.class), any(Integer.class)))
			.thenReturn(false);
		doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			Page mockPage = (Page) args[0];
			String mockTitle = (String) args[1];
			String mockContent = (String) args[2];
			mockPage.update(mockTitle, mockContent);
			return null;
		})
			.when(pageService)
			.update(any(Page.class), any(String.class), any(String.class));

		//when
		String newTitle = prefix + "newtitle";
		String newHeading = prefix + "newheading";
		String newParagraph = prefix + "newparagraph";
		String newContent = String.format("# %s\n\n%s", newHeading, newParagraph);
		String newSummary = prefix + "newsummary";
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(newContent)
			.summary(newSummary)
			.build();

		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals(newTitle, page.getTitle());
		assertEquals(newContent, page.getContent());
		assertTrue(page.getHtml().contains(newHeading));
		assertTrue(page.getHtml().contains(newParagraph));
		assertEquals("/wiki/" + newTitle, pageEdit.getRedirect());
	}

	@Test
	void testWikiPageEditPageButNewTitleExists() {
		//given
		String prefix = "editpagebutnewtitleexists";
		String title = prefix + "title";
		String newTitle = prefix + "newtitle";
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		String summary = prefix + "summary";
		Page page = createPage(title, content);
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary(summary)
			.build();

		createPage(newTitle, content);
		when(pageService.checkLock(title))
			.thenReturn(false);
		when(pageService.checkLock(eq(title), any(LocalDateTime.class), any(Integer.class)))
			.thenReturn(false);
		when(pageService.create(title, content))
			.thenReturn(Page.builder()
				.title(title)
				.content(content)
				.build()
			);

		//when
		PageEdit pageEdit = wikiService.edit(title, request);

		//then
		assertEquals(title, pageEdit.getTitle());
		assertEquals(newTitle, pageEdit.getNewTitle());
		assertEquals(content, pageEdit.getContent());
		assertTrue(pageEdit.getHtml().contains(heading));
		assertTrue(pageEdit.getHtml().contains(paragraph));
		assertEquals(null, pageEdit.getRedirect());
	}

	@Test
	void testWikiPageHistoryNoPage() {
		//given
		String prefix = "historynopage";
		String title = prefix + "title";
		Integer current = 5;
		when(pageService.find(title))
			.thenReturn(null);

		//when
		PageHistoryView history = wikiService.history(title, current);

		//then
		assertEquals("/wiki/" + title, history.getRedirect());
	}

	@Test
	void testWikiPageHistory() {
		//given
		String prefix = "history";
		String title = prefix + "title";
		String content = prefix + "content";
		int current = 2;
		int last = 3;
		int number = 25;
		Page page = createPage(title, content);
		createHistorys(page, number);

		//when
		PageHistoryView history = wikiService.history(title, current);

		//then
		assertEquals(title, history.getTitle());
		assertEquals(current, history.getCurrent());
		assertEquals(last, history.getLast());
		assertEquals(10, history.getHistorys().size());
	}

	@Test
	void testWikiPageHistoryLastPage() {
		//given
		String prefix = "historylastpage";
		String title = prefix + "title";
		String content = prefix + "content";
		int current = 4;
		int last = 4;
		int number = 33;
		Page page = createPage(title, content);
		createHistorys(page, number);

		//when
		PageHistoryView history = wikiService.history(title, current);

		//then
		assertEquals(title, history.getTitle());
		assertEquals(current, history.getCurrent());
		assertEquals(last, history.getLast());
		assertEquals(3, history.getHistorys().size());
	}

	@Test
	void testWikiPageBack() {
		//given
		String prefix = "back";
		String title = prefix + "title";
		String content = prefix + "content";
		int number = 5;
		int event = 3;
		Page page = createPage(title, content);
		createHistorys(page, number);

		//when
		when(pageService.create(any(String.class), any(String.class)))
			.thenAnswer(invocation -> {
				String backTitle = (String) invocation.getArguments()[0];
				String backContent = (String) invocation.getArguments()[1];
				return Page.builder()
					.title(backTitle)
					.content(backContent)
					.build();
			});
		PageBack back = wikiService.back(title, event);

		//then
		assertEquals(event, back.getEvent());
	}

	@Test
	void testWikiPageBackNoPage() {
		//given
		String prefix = "backnopage";
		String title = prefix + "title";
		Integer event = 3;
		when(pageService.find(title))
			.thenReturn(null);

		//when
		PageBack back = wikiService.back(title, event);

		//then
		assertEquals("/history/" + title, back.getRedirect());
	}

	@Test
	void testWikiRehash() {
		//given
		String prefix = "rehash";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);

		String oldTitle = prefix + "oldtitle";
		String oldContent = prefix + "oldcontent";
		updatePage(page, oldTitle, oldContent, 1);

		String newTitle = prefix + "newtitle";
		String newContent = prefix + "newcontent";
		updatePage(page, newTitle, newContent, 2);

		//when
		PageRehash rehash = wikiService.rehash(newTitle, 1);

		//then
		assertEquals("/wiki/" + newTitle, rehash.getRedirect());
	}

	@Test
	void testWikiPageDiff() {
		//given
		String title = "difftitle";
		String content = "diffcontent";
		Page page = createPage(title, content);

		String newTitle = "12345";
		String newContent = "67890";
		Integer event = 2;
		History history = History.builder()
		   .page(page)
		   .event(event)
		   .summary("create")
		   .title(newTitle)
		   .content(newContent)
		   .build();
		when(historyService.find(any(Page.class), eq(event)))
			.thenAnswer(invocation -> {
				Page p = (Page) invocation.getArguments()[0];
				if (p.getId().equals(page.getId())) {
					return history;
				}
				return null;
			});

		//when
		PageDiff diff = wikiService.diff(title, event);

		//then
		assertEquals(event, diff.getEvent());
		assertTrue(diff.getTitleDiff().contains("+" + newTitle));
		assertTrue(diff.getTitleDiff().contains("-" + title));
		assertTrue(diff.getContentDiff().contains("+" + newContent));
		assertTrue(diff.getContentDiff().contains("-" + content));
	}

}
