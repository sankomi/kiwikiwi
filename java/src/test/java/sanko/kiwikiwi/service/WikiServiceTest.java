package sanko.kiwikiwi.service;

import java.util.*; //List, Arrays
import java.time.LocalDateTime;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.*; //when, doAnswer
import static org.junit.jupiter.api.Assertions.*; //assertEquals, assertTrue, assertThrows
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import sanko.kiwikiwi.domain.page.Page;
import sanko.kiwikiwi.domain.history.History;
import sanko.kiwikiwi.dto.*; //PageView, PageEditRequest, PageEdit

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
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary(null)
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
	void testWikiPageEditNoPageButTitleExists() {
		//given
		String prefix = "editnopagebuttitleexists";
		String title = prefix + "title";
		String newTitle = prefix + "newtitle";
		String heading = prefix + "heading";
		String paragraph = prefix + "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary(null)
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
		PageEditRequest request = PageEditRequest.builder()
			.title(title)
			.content(newContent)
			.summary(null)
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
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(newContent)
			.summary(null)
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
		Page page = createPage(title, content);
		PageEditRequest request = PageEditRequest.builder()
			.title(newTitle)
			.content(content)
			.summary(null)
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

}
