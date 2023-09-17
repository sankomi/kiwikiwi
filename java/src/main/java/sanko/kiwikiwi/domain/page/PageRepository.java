package sanko.kiwikiwi.domain.page;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, Long> {

	Page findOneByTitle(String title);
	Page findOneByTitleAndLockAndLockId(String title, LocalDateTime lock, Integer lockId);

}
