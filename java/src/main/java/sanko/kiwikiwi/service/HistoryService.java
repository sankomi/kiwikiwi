package sanko.kiwikiwi.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.*; //Transactional, Propagation

import sanko.kiwikiwi.domain.page.Page;
import sanko.kiwikiwi.domain.history.*; //History, HistoryRepository

@RequiredArgsConstructor
@Service
public class HistoryService {

	private final HistoryRepository historyRepository;

	@Transactional
	public void save(Page page, String title, String summary, String content) {
		History last = historyRepository.findFirstByPageOrderByEventDesc(page);
		Integer event = 0;
		if (last != null) {
			event = last.getEvent() + 1;
		}

		historyRepository.save(History.builder()
			.page(page)
			.event(event)
			.title(title)
			.summary(summary)
			.content(content)
			.build());
	}

}
