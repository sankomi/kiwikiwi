package sanko.kiwikiwi.service;

import java.util.regex.*; //Pattern, Matcher

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import sanko.kiwikiwi.domain.page.*; //Page, PageRepository
import sanko.kiwikiwi.domain.history.*; //History, HistoryRepository
import sanko.kiwikiwi.dto.*; //PageView, PageEditRequest, PageEdit
import sanko.kiwikiwi.Constants;

@RequiredArgsConstructor
@Service
public class WikiService {

	private final PageRepository pageRepository;
	private final HistoryRepository historyRepository;

	private boolean match(String string, String regex) {
		return Pattern.compile(regex).matcher(string).find();
	}

	public PageView view(String title) {
		Page page = pageRepository.findOneByTitle(title);

		if (page == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return null;
		}

		return new PageView(page);
	}

	public PageEdit viewEdit(String title) {
		Page page = pageRepository.findOneByTitle(title);

		if (page == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			page = Page.builder()
				.title(title)
				.content("")
				.build();
			return new PageEdit(page);
		}

		return new PageEdit(page);
	}

	@Transactional
	public PageEdit edit(String title, PageEditRequest request) {
		String newTitle = request.getTitle();
		String content = request.getContent();
		String summary = request.getSummary();

		Page page = pageRepository.findOneByTitle(title);
		Page newPage = pageRepository.findOneByTitle(newTitle);

		if (page == null) {
			page = Page.builder()
				.title("")
				.content("")
				.build();
			History history = History.builder()
				.page(page)
				.event(0)
				.title(newTitle)
				.content(content)
				.build();
			page.update(newTitle, content);

			if (newPage == null) {
				pageRepository.save(page);
				historyRepository.save(history);
				return new PageEdit("/wiki/" + newTitle);
			} else {
				return new PageEdit(page, title);
			}
		} else {
			if (title.equals(newTitle)) {
				History history = History.builder()
					.page(page)
					.event(0)
					.title(title)
					.content(content)
					.build();
				page.updateContent(content);
				historyRepository.save(history);
				return new PageEdit("/wiki/" + title);
			} else {
				if (newPage == null) {
					History history = History.builder()
						.page(page)
						.event(0)
						.title(newTitle)
						.content(content)
						.build();
					page.update(newTitle, content);
					historyRepository.save(history);
					return new PageEdit("/wiki/" + newTitle);
				} else {
					page = Page.builder()
						.title(newTitle)
						.content(content)
						.build();
					return new PageEdit(page, title);
				}
			}
		}
	}

}
