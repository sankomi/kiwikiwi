package sanko.kiwikiwi.domain.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageTest {

	@Test
	void testPageBuilder() {
		//given
		String title = "title";
		String content = "content";

		//when
		Page page = Page.builder()
			.title(title)
			.content(content)
			.build();

		//then
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
	}

}
