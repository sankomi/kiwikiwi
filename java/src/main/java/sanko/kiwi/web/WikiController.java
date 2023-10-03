package sanko.kiwi.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*; //GetMapping, PathVariable, RequestParam
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;

import sanko.kiwi.service.WikiService;
import sanko.kiwi.dto.*; //PageView, PageEditRequest, PageEdit, PageHistoryView, PageBack, PageRehash, PageDiff

@RequiredArgsConstructor
@Controller
public class WikiController {

	private final WikiService wikiService;

	@GetMapping("/search")
	public String search(@RequestParam("s") String string, @RequestParam(value = "p", required = false) Integer page, Model model) {
		if (page == null) {
			page = 1;
		}

		PageSearch pageSearch = wikiService.search(string, page);
		model.addAttribute("page", pageSearch);
		return "search";
	}

	@GetMapping("/wiki")
	public String view() {
		String title = wikiService.getRandomPage();
		return "redirect:/wiki/" + title;
	}

	@GetMapping("/wiki/{title}")
	public String view(@PathVariable("title") String title, Model model) {
		PageView pageView = wikiService.view(title);

		if (pageView == null) {
			model.addAttribute("title", title);
			return "not-exist";
		}

		model.addAttribute("page", pageView);
		return "view";
	}

	@GetMapping("/edit/{title}")
	public String viewEdit(@PathVariable("title") String title, Model model) {
		PageEdit pageEdit = wikiService.viewEdit(title);
		model.addAttribute("page", pageEdit);
		return "edit";
	}

	@PostMapping("/edit/{title}")
	public String edit(@PathVariable("title") String title, PageEditRequest request, Model model) {
		PageEdit pageEdit = wikiService.edit(title, request);

		String redirect = pageEdit.getRedirect();
		if (redirect != null) {
			return "redirect:" + redirect;
		}

		model.addAttribute("page", pageEdit);
		return "edit";
	}

	@GetMapping("/history/{title}")
	public String history(@PathVariable("title") String title, Model model) {
		return history(title, 1, model);
	}

	@GetMapping("/history/{title}/{current}")
	public String history(@PathVariable("title") String title, @PathVariable("current") Integer current, Model model) {
		PageHistoryView pageHistoryView = wikiService.history(title, current);

		String redirect = pageHistoryView.getRedirect();
		if (redirect != null) {
			return "redirect:" + redirect;
		}

		model.addAttribute("page", pageHistoryView);
		return "history";
	}

	@GetMapping("/back/{title}/{event}")
	public String back(@PathVariable("title") String title, @PathVariable("event") Integer event, Model model) {
		PageBack pageBack = wikiService.back(title, event);

		String redirect = pageBack.getRedirect();
		if (redirect != null) {
			return "redirect:" + redirect;
		}

		model.addAttribute("page", pageBack);
		return "back";
	}

	@GetMapping("/rehash/{title}/{event}")
	public String rehash(@PathVariable("title") String title, @PathVariable("event") Integer event, Model model) {
		PageRehash pageRehash = wikiService.rehash(title, event);

		String redirect = pageRehash.getRedirect();
		if (redirect != null) {
			return "redirect:" + redirect;
		}

		model.addAttribute("page", pageRehash);
		return "back";
	}

	@GetMapping("/diff/{title}/{event}")
	public String diff(@PathVariable("title") String title, @PathVariable("event") Integer event, Model model) {
		PageDiff pageDiff = wikiService.diff(title, event);

		String redirect = pageDiff.getRedirect();
		if (redirect != null) {
			return "redirect:" + redirect;
		}

		model.addAttribute("page", pageDiff);
		return "diff";
	}

}
