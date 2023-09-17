package sanko.kiwikiwi.domain.history;

import org.springframework.data.jpa.repository.JpaRepository;

import sanko.kiwikiwi.domain.page.Page;

public interface HistoryRepository extends JpaRepository<History, Long> {

	History findFirstByPageOrderByEventDesc(Page page);

}
