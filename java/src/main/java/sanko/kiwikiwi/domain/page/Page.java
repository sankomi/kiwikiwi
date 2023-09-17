package sanko.kiwikiwi.domain.page;

import java.util.List;
import java.time.LocalDateTime;
import jakarta.persistence.*; //Entity, Table, Id, Column, GeneratedValue, GenerationType, OneToMany, OrderBy

import lombok.*; //Builder, Getter, NoArgsConstructor
import org.springframework.data.annotation.LastModifiedDate;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import sanko.kiwikiwi.domain.history.History;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "pages")
public class Page {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", length = 50, unique = true, nullable = false)
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "html")
	private String html;

	@Column(name = "text")
	private String text;

	@Column(name = "lock")
	private LocalDateTime lock;

	@Column(name = "lock_id")
	private Integer lockId;

	@LastModifiedDate
	@Column(name = "refresh")
	private LocalDateTime refresh;

	@OneToMany(mappedBy = "page")
	@OrderBy("id DESC")
	private List<History> historys;

	@Builder
	public Page(String title, String content) {
		this.title = title;
		this.content = content;
		Parser parser = Parser.builder().build();
		Node node = parser.parse(content);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		this.html = renderer.render(node);
	}

	public void update(String title, String content) {
		updateTitle(title);
		updateContent(content);
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateContent(String content) {
		this.content = content;
		Parser parser = Parser.builder().build();
		Node node = parser.parse(content);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		this.html = renderer.render(node);
	}

	public void lock(LocalDateTime lock, Integer lockId) {
		this.lock = lock;
		this.lockId = lockId;
	}

	public void unlock() {
		lock = null;
		lockId = null;
	}

}
