package sanko.kiwikiwi.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*; //assertEquals, assertTrue

import sanko.kiwikiwi.domain.page.*; //Page, PageRepository
import sanko.kiwikiwi.dto.*; //PageView, PageEditRequest, PageEdit

@ExtendWith(SpringExtension.class)
@Import(WikiService.class)
class WikiServiceTest {

	@Autowired
	private WikiService wikiService;

	@MockBean
	private PageRepository pageRepository;

	private static Long pageId = 0L;

	private Page createPage(String title, String content) {
		Page page = new Page(title, content);
		setField(page, "id", ++pageId);
		when(pageRepository.findOneByTitle(title))
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

		when(pageRepository.findOneByTitle(title))
			.thenReturn(null);

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

		when(pageRepository.findOneByTitle(title))
			.thenReturn(null);
		createPage(newTitle, content);

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
