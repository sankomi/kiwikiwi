package sanko.kiwikiwi.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*; //GetMapping, PathVariable
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;

import sanko.kiwikiwi.service.WikiService;
import sanko.kiwikiwi.dto.PageView;

@RequiredArgsConstructor
@Controller
public class WikiController {

	private final WikiService wikiService;

	@GetMapping("/wiki/{title}")
	public String view(@PathVariable("title") String title, Model model) {
		PageView pageView = wikiService.view(title);
		model.addAttribute("page", pageView);
		return "view";
	}

}
