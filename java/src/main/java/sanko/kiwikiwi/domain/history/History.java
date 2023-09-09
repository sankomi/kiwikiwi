package sanko.kiwikiwi.domain.history;

import java.time.LocalDateTime;
import jakarta.persistence.*; //Entity, Table, Id, Column, GeneratedValue, GenerationType, JoinColumn, ManyToOne

import lombok.*; //Builder, Getter, NoArgsConstructor
import org.springframework.data.annotation.CreatedDate;

import sanko.kiwikiwi.domain.page.Page;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "historys")
public class History {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event", columnDefinition = "INTEGER DEFAULT 1")
	private Integer event;

	@JoinColumn(name = "page_id", nullable = false)
	@ManyToOne
	private Page page;

	@Column(name = "summary", length = 100)
	private String summary;

	@Column(name = "title")
	private String title;

	@Column(name = "content")
	private String content;

	@CreatedDate
	@Column(name = "write")
	private LocalDateTime write;

	@Builder
	public History(Integer event, String summary, String title, String content) {
		this.event = event;
		this.summary = summary;
		this.title = title;
		this.content = content;
	}
}
