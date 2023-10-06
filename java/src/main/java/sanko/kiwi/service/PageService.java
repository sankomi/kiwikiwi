package sanko.kiwi.service;

import java.time.LocalDateTime;
import java.util.*; //List, Random

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.*; //Transactional, Propagation

import sanko.kiwi.domain.page.*; //Page, PageRepository

@RequiredArgsConstructor
@Service
public class PageService {

	private final PageRepository pageRepository;

	public Page create() {
		return create("", "");
	}

	public Page create(String title, String content) {
		return Page.builder()
			.title(title)
			.content(content)
			.build();
	}

	public List<Page> search(String title, Integer page) {
		return pageRepository.findByTitleContainingOrTextContaining(title, title);
	}

	public Page getRandomPage() {
		List<Page> pages = pageRepository.findAll();

		int length = pages.size();
		if (length == 0) {
			return null;
		}

		Random random = new Random();
		int index = random.nextInt(length);
		return pages.get(index);
	}

	public Page find(String title) {
		return pageRepository.findOneByTitle(title);
	}

	@Transactional
	public void save(Page page) {
		pageRepository.save(page);
	}

	@Transactional
	public void update(Page page, String title, String content) {
		page.update(title, content);
		page.unlock();
	}

	@Transactional
	public boolean checkLock(String title) {
		Page page = pageRepository.findOneByTitle(title);
		if (page == null) return false;

		LocalDateTime lock = page.getLock();
		if (lock == null) return false;
		if (lock.isAfter(LocalDateTime.now())) return true;

		unlock(page);
		return false;
	}

	public boolean checkLock(String title, LocalDateTime lock, Integer lockId) {
		Page page = pageRepository.findOneByTitleAndLockAndLockId(title, lock, lockId);
		return page == null;
	}

	@Transactional
	public void lock(Page page, LocalDateTime lock, Integer lockId) {
		page.lock(lock, lockId);
	}

	@Transactional
	public void unlock(Page page) {
		page.unlock();
	}

}
