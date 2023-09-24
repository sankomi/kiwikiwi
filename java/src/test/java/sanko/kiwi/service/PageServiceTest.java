package sanko.kiwi.service;

import java.util.Random;
import java.time.LocalDateTime;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.Mockito.*; //when, verify, times, spy
import static org.junit.jupiter.api.Assertions.*; //assertEquals, assertTrue, assertFalse, assertNull, assertNotNull

import sanko.kiwi.domain.page.*; //Page, PageRepository

@ExtendWith(SpringExtension.class)
@Import(PageService.class)
class PageServiceTest {

	@Autowired
	private PageService pageService;

	@MockBean
	private PageRepository pageRepository;

	private static Long pageId = 0L;

	private Page createPage(String title, String content) {
		Page page = new Page(title, content);
		setField(page, "id", ++pageId);
		when(pageRepository.findOneByTitle(title))
			.thenReturn(page);
		return page;
	}

	@Test
	void testCreatePageNoArgs() {
		//when
		Page page = pageService.create();

		//then
		assertNotNull(page);
		assertEquals("", page.getTitle());
		assertEquals("", page.getContent());
	}

	@Test
	void testCreatePage() {
		//given
		String prefix = "createpage";
		String title = prefix + "title";
		String content = prefix + "content";

		//when
		Page page = pageService.create(title, content);

		//then
		assertNotNull(page);
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
	}

	@Test
	void testFindPageNoPage() {
		//given
		String prefix = "findnopage";
		String title = prefix + "title";
		when(pageRepository.findOneByTitle(title))
			.thenReturn(null);

		//when
		Page page = pageService.find(title);

		//then
		assertNull(page);
	}

	@Test
	void testFindPage() {
		//given
		String prefix = "findpage";
		String title = prefix + "title";
		String content = prefix + "content";
		createPage(title, content);

		//when
		Page page = pageService.find(title);

		//then
		assertNotNull(page);
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
	}

	@Test
	void testSavePage() {
		//given
		String prefix = "savepage";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = new Page(title, content);

		//when
		pageService.save(page);

		//then
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
		verify(pageRepository, times(1)).save(page);
	}

	@Test
	void testUpdatePage() {
		//given
		String prefix = "updatepage";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = spy(createPage(title, content));

		//when
		String newTitle = prefix + "newtitle";
		String newContent = prefix + "newcontent";
		pageService.update(page, newTitle, newContent);

		//then
		assertEquals(newTitle, page.getTitle());
		assertEquals(newContent, page.getContent());
		verify(page, times(1)).unlock();
	}

	@Test
	void testCheckLockNotLocked() {
		//given
		String prefix = "checklock";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);

		//when
		boolean locked = pageService.checkLock(title);

		//then
		assertFalse(locked);
	}

	@Test
	void testCheckLockPageNotExist() {
		//given
		String title = "checklockpagenotexist" + "title";
		when(pageRepository.findOneByTitle(title))
			.thenReturn(null);

		//when
		boolean locked = pageService.checkLock(title);

		//then
		assertFalse(locked);
	}

	@Test
	void testCheckLockLocked() {
		//given
		String prefix = "checklocklocked";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);

		LocalDateTime lock = LocalDateTime.now().plusSeconds(60);
		Random random = new Random();
		Integer lockId = random.nextInt(2147483647);
		page.lock(lock, lockId);

		//when
		boolean locked = pageService.checkLock(title);

		//then
		assertTrue(locked);
	}

	@Test
	void testCheckLockLocker() {
		//given
		String prefix = "checklocklocker";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);

		LocalDateTime lock = LocalDateTime.now().plusSeconds(60);
		Random random = new Random();
		Integer lockId = random.nextInt(2147483647);
		page.lock(lock, lockId);
		when(pageRepository.findOneByTitleAndLockAndLockId(title, lock, lockId))
			.thenReturn(page);

		//when
		boolean locked = pageService.checkLock(title, lock, lockId);

		//then
		assertFalse(locked);
	}

	@Test
	void testCheckLockNotLocker() {
		//given
		String prefix = "checklocknotlocker";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = createPage(title, content);

		LocalDateTime lock = LocalDateTime.now().plusSeconds(60);
		Random random = new Random();
		Integer lockId = random.nextInt(2147483647);
		page.lock(lock, lockId);
		when(pageRepository.findOneByTitleAndLockAndLockId(title, lock, lockId))
			.thenReturn(null);

		//when
		boolean locked = pageService.checkLock(title, lock, lockId);

		//then
		assertTrue(locked);
	}

	@Test
	void testLockPage() {
		//given
		String prefix = "lockpage";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = spy(createPage(title, content));

		//when
		LocalDateTime lock = LocalDateTime.now().plusSeconds(60);
		Random random = new Random();
		Integer lockId = random.nextInt(2147483647);

		pageService.lock(page, lock, lockId);

		//then
		assertEquals(lock, page.getLock());
		assertEquals(lockId, page.getLockId());
		verify(page, times(1)).lock(lock, lockId);
	}

	@Test
	void testUnlockPage() {
		//given
		String prefix = "unlockpage";
		String title = prefix + "title";
		String content = prefix + "content";
		Page page = spy(createPage(title, content));

		//when
		pageService.unlock(page);

		//then
		assertEquals(null, page.getLock());
		assertEquals(null, page.getLockId());
		verify(page, times(1)).unlock();
	}

}
