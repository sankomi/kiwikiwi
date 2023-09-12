package sanko.kiwikiwi.web;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Test;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; //status. view, model
import static org.hamcrest.Matchers.*; //hasProperty, equalTo

import sanko.kiwikiwi.domain.page.Page;
import sanko.kiwikiwi.dto.PageView;
import sanko.kiwikiwi.service.WikiService;

@WebMvcTest(WikiController.class)
class WikiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private WikiService wikiService;

	private static Long pageId = 0L;

	private void createPage(String title, String content) {
		Page page = new Page(title, content);
		setField(page, "id", ++pageId);
		when(wikiService.view(title))
			.thenReturn(new PageView(page));
	}

	@Test
	void testWikiPageView() throws Exception {
		//given
		String title = "viewtitle";
		String content = "viewcontent";
		createPage(title, content);

		//whenthen
		mockMvc.perform(get("/wiki/" + title))
			.andExpect(status().isOk())
			.andExpect(view().name("view"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))));
	}

}
