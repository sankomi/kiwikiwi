package sanko.kiwi.domain.page;

import java.util.Optional;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaSystemException;

import static org.junit.jupiter.api.Assertions.*; //assertTrue, assertEquals, assertThrows

@DataJpaTest
class PageRepositoryTest {

	@Autowired
	private PageRepository pageRepository;

	@Test
	void testPageSave() {
		//given
		String title = "savetitle";
		String content = "savecontent";
		Page page = new Page(title, content);

		//when
		pageRepository.save(page);

		//then
		assertTrue(page.getId() > 0L);
		assertEquals(title, page.getTitle());
		assertEquals(content, page.getContent());
	}

	@Test
	void testPageFind() {
		//given
		String title = "findtitle";
		String content = "findcontent";
		Page page = new Page(title, content);
		pageRepository.save(page);
		Long id = page.getId();

		//when
		Optional<Page> found = pageRepository.findById(id);

		//then
		assertTrue(found.isPresent());
		found.ifPresent(p -> {
			assertEquals(id, p.getId());
			assertEquals(title, p.getTitle());
			assertEquals(content, p.getContent());
		});
	}

	@Test
	void testPageDuplicateTitle() {
		//given
		String title = "duplicatetitle";
		String content = "duplicatecontent";
		Page page = new Page(title, content);
		pageRepository.save(page);

		//when
		String duplicateTitle = title;
		String duplicateContent = content;
		Page duplicate = new Page(title, content);

		//then
		JpaSystemException exception = assertThrows(JpaSystemException.class, () -> pageRepository.save(duplicate));
		assertTrue(exception.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE"));
	}

	@Test
	void testFindOneByTitle() {
		//given
		String title = "findtitle";
		String content = "findcontent";
		Page page = new Page(title, content);
		pageRepository.save(page);

		//when
		Page found = pageRepository.findOneByTitle(title);

		//then
		assertEquals(title, found.getTitle());
		assertEquals(content, found.getContent());
	}

}
