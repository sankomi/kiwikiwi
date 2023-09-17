package sanko.kiwikiwi.service;

import java.util.Random;
import java.util.regex.*; //Pattern, Matcher
import java.time.LocalDateTime;

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
	private final PageService pageService;
	private final HistoryService historyService;

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

	public PageEdit edit(String title, PageEditRequest request) {
		String newTitle = request.getTitle();
		String content = request.getContent();
		String summary = request.getSummary();

		if (match(newTitle, Constants.TITLE_REGEX)) {
			newTitle = newTitle.replaceAll(Constants.TITLE_REGEX, "");
			Page page = Page.builder()
				.title(title)
				.content(content)
				.build();
			return new PageEdit(page, newTitle, summary);
		}

		try {
			Page updated = update(title, newTitle, content, summary);
			return new PageEdit("/wiki/" + updated.getTitle());
		} catch (TitleDuplicateException | PageLockException e) {
			Page page = Page.builder()
				.title(title)
				.content(content)
				.build();
			return new PageEdit(page, newTitle, summary);
		}
	}

	private class TitleDuplicateException extends Exception {

		public TitleDuplicateException(String message) {
			super(message);
		}

	}

	private class PageLockException extends Exception {

		public PageLockException(String message) {
			super(message);
		}

	}

	private Page update(String title, String newTitle, String content, String summary) throws TitleDuplicateException, PageLockException {
		Page page = pageRepository.findOneByTitle(title);

		if (!title.equals(newTitle)) {
			Page newPage = pageRepository.findOneByTitle(newTitle);

			if (newPage != null) {
				throw new TitleDuplicateException("page with new title already exists");
			}
		}

		if (page == null) {
			page = Page.builder()
				.title("")
				.content("")
				.build();

			historyService.save(page, title, summary, content);
			pageService.save(page, title, content);
			return page;
		} else {
			if (pageService.checkLock(title)) {
				throw new PageLockException("page is locked");
			}

			LocalDateTime lock = LocalDateTime.now().plusSeconds(60);
			Random random = new Random();
			Integer lockId = random.nextInt(2147483647);
			pageService.lock(page, lock, lockId);

			if (pageService.checkLock(title, lock, lockId)) {
				throw new PageLockException("page is locked");
			}

			historyService.save(page, title, summary, content);
			pageService.update(page, newTitle, content);
			return page;
		}
	}

}
