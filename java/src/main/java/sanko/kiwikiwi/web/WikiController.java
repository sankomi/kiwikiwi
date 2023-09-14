package sanko.kiwikiwi.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*; //GetMapping, PathVariable
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;

import sanko.kiwikiwi.service.WikiService;
import sanko.kiwikiwi.dto.*; //PageView, PageEditRequest, PageEdit

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

}
