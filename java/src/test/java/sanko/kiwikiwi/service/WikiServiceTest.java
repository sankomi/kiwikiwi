package sanko.kiwikiwi.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import sanko.kiwikiwi.domain.page.*; //Page, PageRepository
import sanko.kiwikiwi.dto.PageView;

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
		String content = "viewcontent";
		Page page = createPage(title, content);

		//when
		PageView pageView = wikiService.view(title);

		//then
		assertEquals(title, pageView.getTitle());
	}

}
