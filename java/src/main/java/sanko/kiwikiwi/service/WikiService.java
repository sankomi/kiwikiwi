package sanko.kiwikiwi.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import sanko.kiwikiwi.domain.page.*; //Page, PageRepository
import sanko.kiwikiwi.dto.*; //PageView, PageEditRequest, PageEdit

@RequiredArgsConstructor
@Service
public class WikiService {

	private final PageRepository pageRepository;

	public PageView view(String title) {
		Page page = pageRepository.findOneByTitle(title);

		return new PageView(page);
	}

	public PageEdit viewEdit(String title) {
		Page page = pageRepository.findOneByTitle(title);

		return new PageEdit(page);
	}

	@Transactional
	public PageEdit edit(String title, PageEditRequest request) {
		String newTitle = request.getTitle();
		String content = request.getContent();
		String summary = request.getSummary();

		Page page = pageRepository.findOneByTitle(title);
		Page newPage = pageRepository.findOneByTitle(newTitle);

		if (page == null) {
			page = Page.builder()
				.title(newTitle)
				.content(content)
				.build();

			if (newPage == null) {
				pageRepository.save(page);
				return new PageEdit("/wiki/" + newTitle);
			} else {
				return new PageEdit(page, title);
			}
		} else {
			if (title.equals(newTitle)) {
				page.updateContent(content);
				return new PageEdit("/wiki/" + title);
			} else {
				if (newPage == null) {
					page.updateTitle(newTitle);
					page.updateContent(content);
					return new PageEdit("/wiki/" + newTitle);
				} else {
					page = Page.builder()
						.title(newTitle)
						.content(content)
						.build();
					return new PageEdit(page, title);
				}
			}
		}
	}

}
