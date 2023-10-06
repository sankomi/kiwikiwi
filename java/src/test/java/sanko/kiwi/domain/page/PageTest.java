package sanko.kiwi.domain.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*; //assertEquals, assertTrue

class PageTest {

	@Test
	void testPageBuilder() {
		//given
		String title = "title";
		String heading = "heading";
		String paragraph = "paragraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);

		//when
		Page page = Page.builder()
			.title(title)
			.content(content)
			.build();

		//then
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
		assertTrue(page.getHtml().contains("<h1>"));
		assertTrue(page.getHtml().contains("<p>"));
		assertTrue(page.getHtml().contains(heading));
		assertTrue(page.getHtml().contains(paragraph));
	}

	@Test
	void testPageLink() {
		//given
		String prefix = "pagelink";
		String title = prefix + "title";
		String content = "[[test]]";

		//when
		Page page = Page.builder()
			.title(title)
			.content(content)
			.build();

		//then
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
		assertTrue(page.getHtml().contains("<a"));
		assertTrue(page.getHtml().contains("/wiki/test"));
	}

	@Test
	void testPageUpdate() {
		//given
		String title = "updatetitle";
		String heading = "updateheading";
		String paragraph = "updateparagraph";
		String content = String.format("# %s\n\n%s", heading, paragraph);
		Page page = Page.builder()
			.title(title)
			.content(content)
			.build();

		//when
		String newTitle = "updatenewtitle";
		String newHeading = "updatenewheading";
		String newParagraph = "updatenewparagraph";
		String newContent = String.format("# %s\n\n%s", newHeading, newParagraph);
		page.update(newTitle, newContent);

		//then
		assertEquals(newTitle, page.getTitle());
		assertEquals(newContent, page.getContent());
		assertTrue(page.getHtml().contains("<h1>"));
		assertTrue(page.getHtml().contains("<p>"));
		assertTrue(page.getHtml().contains(newHeading));
		assertTrue(page.getHtml().contains(newParagraph));
	}

}
