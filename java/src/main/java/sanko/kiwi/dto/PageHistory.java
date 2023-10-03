package sanko.kiwi.dto;

import java.time.LocalDateTime;

import lombok.Getter;

import sanko.kiwi.domain.history.History;

@Getter
public class PageHistory {

	private String summary;
	private LocalDateTime write;
	private Integer event;

	public PageHistory(History history) {
		this.summary = history.getSummary();
		this.write = history.getWrite();
		this.event = history.getEvent();
	}

}
