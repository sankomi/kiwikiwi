package sanko.kiwi.web;

import java.util.*; //List, ArrayList

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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import sanko.kiwi.domain.page.Page;
import sanko.kiwi.domain.history.History;
import sanko.kiwi.dto.*; //PageView, PageEdit, PageBack, PageRehash, PageDiff
import sanko.kiwi.service.WikiService;

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

	private History createHistory(Page page, String title, String content, String summary, Integer event) {
		return History.builder()
			.page(page)
			.event(event)
			.summary(summary)
			.title(title)
			.content(content)
			.build();
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
		}
		setField(page, "historys", historys);
		return historys;
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

	@Test
	void testWikiHistoryView() throws Exception {
		//given
		String prefix = "historyview";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);
		int number = 13;
		int last = 2;
		List<History> historys = createHistorys(page, number);

		when(wikiService.history(title, 1))
			.thenReturn(new PageHistoryView(page, 1, last, historys.subList(0, 10)));

		//whenthen
		mockMvc.perform(get("/history/" + title))
			.andExpect(status().isOk())
			.andExpect(view().name("history"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("current", equalTo(1))))
			.andExpect(model().attribute("page", hasProperty("last", equalTo(last))))
			.andExpect(model().attribute("page", hasProperty("historys", hasSize(10))));
	}

	@Test
	void testWikiHistoryViewPageTwo() throws Exception {
		//given
		String prefix = "historyviewpagetwo";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);
		int number = 13;
		int current = 2;
		int last = 2;
		List<History> historys = createHistorys(page, number);

		when(wikiService.history(title, current))
			.thenReturn(new PageHistoryView(page, current, last, historys.subList(10, 13)));

		//whenthen
		mockMvc.perform(get("/history/" + title + "/" + String.valueOf(current)))
			.andExpect(status().isOk())
			.andExpect(view().name("history"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("current", equalTo(current))))
			.andExpect(model().attribute("page", hasProperty("last", equalTo(last))))
			.andExpect(model().attribute("page", hasProperty("historys", hasSize(3))));
	}

	@Test
	void testWikiHistoryViewNoPage() throws Exception {
		//given
		String prefix = "historyviewnopage";
		String title = prefix + "title";

		when(wikiService.history(title, 1))
			.thenReturn(new PageHistoryView("/wiki/" + title));

		//whenthen
		mockMvc.perform(get("/history/" + title))
			.andExpect(status().isMovedTemporarily())
			.andExpect(view().name("redirect:/wiki/" + title));
	}

	@Test
	void testWikiBack() throws Exception {
		//given
		String prefix = "back";
		String title = prefix + "title";
		String content = prefix + "content";
		Integer event = 1;
		Page page = createPage(title, content);

		when(wikiService.back(title, event))
			.thenReturn(new PageBack(page, event));

		//whenthen
		mockMvc.perform(get("/back/" + title + "/" + String.valueOf(event)))
			.andExpect(status().isOk())
			.andExpect(view().name("back"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("html", equalTo(page.getHtml()))))
			.andExpect(model().attribute("page", hasProperty("event", equalTo(event))));
	}

	@Test
	void testWikiBackNoPage() throws Exception {
		//given
		String prefix = "backnopage";
		String title = prefix + "title";
		Integer event = 4;

		when(wikiService.back(title, event))
			.thenReturn(new PageBack("/history/" + title));

		//whenthen
		mockMvc.perform(get("/back/" + title + "/" + String.valueOf(event)))
			.andExpect(status().isMovedTemporarily())
			.andExpect(view().name("redirect:/history/" + title));
	}

	@Test
	void testWikiRehash() throws Exception {
		//given
		String prefix = "rehash";
		String title = prefix + "title";
		String content = prefix + "content";
		Integer event = 2;
		Page page = createPage(title, content);

		when(wikiService.rehash(title, event))
			.thenReturn(new PageRehash("/wiki/" + title));

		//whenthen
		mockMvc.perform(get("/rehash/" + title + "/" + String.valueOf(event)))
			.andExpect(status().isMovedTemporarily())
			.andExpect(view().name("redirect:/wiki/" + title));
	}

	@Test
	void testWikiRehashNoPage() throws Exception {
		//given
		String prefix = "rehashnopage";
		String title = prefix + "title";
		Integer event = 6;

		when(wikiService.rehash(title, event))
			.thenReturn(new PageRehash("/wiki/" + title));

		//whenthen
		mockMvc.perform(get("/rehash/" + title + "/" + String.valueOf(event)))
			.andExpect(status().isMovedTemporarily())
			.andExpect(view().name("redirect:/wiki/" + title));
	}

	@Test
	void testWikiDiff() throws Exception {
		//given
		String prefix = "diff";
		String title = prefix + "title";
		String content = prefix + "content";
		String titleDiff = prefix + "titlediff";
		String contentDiff = prefix + "contentdiff";
		String summary = prefix + "summary";
		Integer event = 3;
		Page page = createPage(title, content);
		History history = createHistory(page, title, content, summary, event);

		when(wikiService.diff(title, event))
			.thenReturn(new PageDiff(title, history, titleDiff, contentDiff));

		//whenthen
		mockMvc.perform(get("/diff/" + title + "/" + String.valueOf(event)))
			.andExpect(status().isOk())
			.andExpect(view().name("diff"))
			.andExpect(model().attribute("page", hasProperty("title", equalTo(title))))
			.andExpect(model().attribute("page", hasProperty("summary", equalTo(summary))))
			.andExpect(model().attribute("page", hasProperty("titleDiff", equalTo(titleDiff))))
			.andExpect(model().attribute("page", hasProperty("contentDiff", equalTo(contentDiff))))
			.andExpect(model().attribute("page", hasProperty("event", equalTo(event))));
	}

}
