package sanko.kiwikiwi.domain.page;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, Long> {

	Page findOneByTitle(String title);

}
