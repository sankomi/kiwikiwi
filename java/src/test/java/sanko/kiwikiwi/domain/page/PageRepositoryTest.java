package sanko.kiwikiwi.domain.page;

import java.util.Optional;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
