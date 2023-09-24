package sanko.kiwi.domain.history;

import org.springframework.data.jpa.repository.JpaRepository;

import sanko.kiwi.domain.page.Page;

public interface HistoryRepository extends JpaRepository<History, Long> {

	History findFirstByPageOrderByEventDesc(Page page);
	History findOneByPageAndEvent(Page page, Integer event);

}
