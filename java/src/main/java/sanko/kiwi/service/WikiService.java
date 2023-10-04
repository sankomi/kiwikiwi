package sanko.kiwi.service;

import java.util.*; //Random, List, Collections, LinkedList
import java.util.regex.*; //Pattern, Matcher
import java.time.LocalDateTime;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.*; //Diff, Patch

import sanko.kiwi.domain.page.Page;
import sanko.kiwi.domain.history.History;
import sanko.kiwi.dto.*; //PageView, PageEditRequest, PageEdit, PageBack, PageRehash, PageDiff, PageSearch
import sanko.kiwi.Constants;

@RequiredArgsConstructor
@Service
public class WikiService {

	private final PageService pageService;
	private final HistoryService historyService;

	private boolean match(String string, String regex) {
		return Pattern.compile(regex).matcher(string).find();
	}

	public PageSearch search(String string, Integer page) {
		if (string == null || string.isEmpty()) {
			return new PageSearch("", 1, 1, null);
		}

		List<Page> pages = pageService.search(string, page);

		int length = pages.size();
		int last = (int) Math.ceil(((float) length) / 10);
		int to = Math.min(page * 10, length);
		List<Page> show = pages.subList((page - 1) * 10, to);

		return new PageSearch(string, page, last, pages);
	}

	public String getRandomPage() {
		Page page = pageService.getRandomPage();

		if (page == null) {
			return "kiwikiwi";
		}

		return page.getTitle();
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

	public PageBack back(String title, Integer event) {
		Page back = make(title, event);

		if (back == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return new PageBack("/history/" + title);
		}

		return new PageBack(back, event);
	}

	public PageRehash rehash(String title, Integer event) {
		Page back = make(title, event);

		if (back == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return new PageRehash("/wiki/" + title);
		}

		try {
			Page updated = update(title, back.getTitle(), back.getContent(), "rehash(" + String.valueOf(event) + ")");
			return new PageRehash("/wiki/" + back.getTitle());
		} catch (TitleDuplicateException | PageLockException e) {
			return new PageRehash("/back/" + title + "/" + String.valueOf(event));
		}
	}

	private Page make(String title, Integer event) {
		Page page = pageService.find(title);

		if (page == null) {
			return null;
		}

		History hist = historyService.find(page, event);

		if (hist == null) {
			return null;
		}

		String backTitle = "";
		String backContent = "";

		List<History> historys = page.getHistorys();
		Collections.reverse(historys);
		for (History history : historys) {
			if (history.getEvent() > event) {
				break;
			}
			backTitle = applyPatch(backTitle, history.getTitle());
			backContent = applyPatch(backContent, history.getContent());
		}

		return pageService.create(backTitle, backContent);
	}

	private String applyPatch(String text, String patchText) {
		DiffMatchPatch dmp = new DiffMatchPatch();
		List<Patch> patch = dmp.patchFromText(patchText);
		Object[] patched = dmp.patchApply(new LinkedList<>(patch), text);
		return (String) patched[0];
	}

	public PageDiff diff(String title, Integer event) {
		Page page = pageService.find(title);

		if (page == null) {
			if (match(title, Constants.TITLE_REGEX)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return new PageDiff("/wiki/" + title);
		}

		History history = historyService.find(page, event);

		if (history == null) {
			return new PageDiff("/history/" + title);
		}

		String titleDiff = history.getTitle();
		titleDiff = titleDiff.replaceAll("\\n\\+([^\\n]*)", "\n##ins##\\%2b$1##/ins##");
		titleDiff = titleDiff.replaceAll("\\n\\-([^\\n]*)", "\n##del##\\-$1##/del##");
		titleDiff = titleDiff.replaceAll("@@\\s\\-\\d+,{0,1}\\d*\\s\\+\\d+,{0,1}\\d*\\s@@\\n{0,1}", "");
		try {
			titleDiff = URLDecoder.decode(titleDiff, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		titleDiff = titleDiff.replaceAll("##(ins|/ins|del|/del)##", "<$1>");

		String contentDiff = history.getContent();
		contentDiff = contentDiff.replaceAll("\\n\\+([^\\n]*)", "\n##ins##%2b$1##/ins##");
		contentDiff = contentDiff.replaceAll("\\n\\-([^\\n]*)", "\n##del##-$1##/del##");
		contentDiff = contentDiff.replaceAll("@@\\s\\-\\d+,{0,1}\\d*\\s\\+\\d+,{0,1}\\d*\\s@@\\n{0,1}", "");
		contentDiff = contentDiff.replaceAll("(%0D)*%0A", "%0A ");
		try {
			contentDiff = URLDecoder.decode(contentDiff, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		contentDiff = contentDiff.replaceAll("##(ins|/ins|del|/del)##", "<$1>");

		return new PageDiff(title, history, titleDiff, contentDiff);
	}

}
