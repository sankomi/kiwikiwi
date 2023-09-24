package sanko.kiwi.service;

import java.util.*; //Random, List
import java.util.regex.*; //Pattern, Matcher
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import sanko.kiwi.domain.page.Page;
import sanko.kiwi.domain.history.History;
import sanko.kiwi.dto.*; //PageView, PageEditRequest, PageEdit
import sanko.kiwi.Constants;

@RequiredArgsConstructor
@Service
public class WikiService {

	private final PageService pageService;
	private final HistoryService historyService;

	private boolean match(String string, String regex) {
		return Pattern.compile(regex).matcher(string).find();
	}

	public PageView view(String title) {
		Page page = pageService.find(title);

		if (page == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return null;
		}

		return new PageView(page);
	}

	public PageEdit viewEdit(String title) {
		Page page = pageService.find(title);

		if (page == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			page = pageService.create(title, "");
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
			Page page = pageService.create(title, content);
			return new PageEdit(page, newTitle, summary);
		}

		try {
			Page updated = update(title, newTitle, content, summary);
			return new PageEdit("/wiki/" + updated.getTitle());
		} catch (TitleDuplicateException | PageLockException e) {
			Page page = pageService.create(title, content);
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
		Page page = pageService.find(title);

		if (!title.equals(newTitle)) {
			Page newPage = pageService.find(newTitle);

			if (newPage != null) {
				throw new TitleDuplicateException("page with new title already exists");
			}
		}

		if (page == null) {
			page = pageService.create();
			pageService.save(page);
			if (summary.isEmpty()) {
				summary = "create";
			}
			historyService.save(page, title, summary, content);
			pageService.update(page, title, content);
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

			if (summary.isEmpty()) {
				summary = "edit";
			}
			historyService.save(page, title, summary, content);
			pageService.update(page, newTitle, content);
			return page;
		}
	}

	public PageHistoryView history(String title, Integer current) {
		Page page = pageService.find(title);

		if (page == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return new PageHistoryView("/wiki/" + title);
		}

		int length = page.getHistorys().size();
		int last = (int) Math.ceil(((float) length) / 10);
		int to = Math.min(current * 10, length);
		List<History> historys = page.getHistorys().subList((current - 1) * 10, to);
		return new PageHistoryView(page, current, last, historys);
	}

}
