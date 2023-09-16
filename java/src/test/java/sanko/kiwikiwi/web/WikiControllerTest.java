package sanko.kiwikiwi.web;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; //get, post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; //status. view, model
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import sanko.kiwikiwi.domain.page.Page;
import sanko.kiwikiwi.dto.*; //PageView, PageEdit
import sanko.kiwikiwi.service.WikiService;

@WebMvcTest(WikiController.class)
class WikiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private WikiService wikiService;

	private static Long pageId = 0L;

	private Page createPage(String title, String content) {
		Page page = new Page(title, content);
		setField(page, "id", ++pageId);
		when(wikiService.view(title))
			.thenReturn(new PageView(page));
		when(wikiService.viewEdit(title))
			.thenReturn(new PageEdit(page));
		return page;
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
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("html", containsString(content))));
	}

	@Test
	void testWikiEditView() throws Exception {
		//given
		String prefix = "editview";
		String title = prefix + "title";
		String content = prefix + "content";
		createPage(title, content);

		//whenthen
		mockMvc.perform(get("/edit/" + title))
			.andExpect(status().isOk())
			.andExpect(view().name("edit"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("html", containsString(content))));
	}

	@Test
	void testWikiEdit() throws Exception {
		//given
		String prefix = "edit";
		String title = prefix + "title";
		String newTitle = title;
		String content = prefix + "content";
		String summary = prefix + "summary";
		createPage(title, content);

		when(wikiService.edit(eq(title), any(PageEditRequest.class)))
			.thenReturn(new PageEdit("/wiki/" + title));

		//whenthen
		mockMvc.perform(
			post("/edit/" + title)
				.param("title", title)
				.param("newTitle", newTitle)
				.param("content", content)
				.param("summary", summary)
		)
			.andExpect(status().isMovedTemporarily())
			.andExpect(view().name("redirect:/wiki/" + title));
	}

	@Test
	void testWikiEditFail() throws Exception {
		//given
		String prefix = "editfail";
		String title = prefix + "title";
		String newTitle = title;
		String content = prefix + "content";
		String summary = prefix + "summary";
		Page page = createPage(title, content);

		when(wikiService.edit(eq(title), any(PageEditRequest.class)))
			.thenReturn(new PageEdit(page, title, summary));

		//whenthen
		mockMvc.perform(
			post("/edit/" + title)
				.param("title", title)
				.param("newTitle", newTitle)
				.param("content", content)
				.param("summary", summary)
		)
			.andExpect(status().isOk())
			.andExpect(view().name("edit"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("html", containsString(content))));
	}

}
