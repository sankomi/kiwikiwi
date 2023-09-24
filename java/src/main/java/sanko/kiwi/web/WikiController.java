package sanko.kiwi.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*; //GetMapping, PathVariable
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;

import sanko.kiwi.service.WikiService;
import sanko.kiwi.dto.*; //PageView, PageEditRequest, PageEdit, PageHistoryView

@RequiredArgsConstructor
@Controller
public class WikiController {

	private final WikiService wikiService;

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

}
