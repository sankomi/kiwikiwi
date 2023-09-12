package sanko.kiwikiwi.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import sanko.kiwikiwi.domain.page.*; //Page, PageRepository
import sanko.kiwikiwi.dto.PageView;

@RequiredArgsConstructor
@Service
public class WikiService {

	private final PageRepository pageRepository;

	public PageView view(String title) {
		Page page = pageRepository.findOneByTitle(title);

		return new PageView(page);
	}

}
