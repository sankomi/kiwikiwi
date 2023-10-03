package sanko.kiwi.domain.history;

import java.time.LocalDateTime;
import java.util.*; //List, LinkedList
import jakarta.persistence.*; //Entity, Table, Id, Column, GeneratedValue, GenerationType, JoinColumn, ManyToOne

import lombok.*; //Builder, Getter, NoArgsConstructor
import org.springframework.data.annotation.CreatedDate;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.*; //Diff, Patch

import sanko.kiwi.domain.page.Page;

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
	public History(Page page, Integer event, String summary, String title, String content) {
		this.page = page;
		this.event = event;
		this.summary = summary;
		this.title = getPatch(page.getTitle(), title);
		this.content = getPatch(page.getContent(), content);
		this.write = LocalDateTime.now();
	}

	private String getPatch(String text1, String text2) {
		DiffMatchPatch dmp = new DiffMatchPatch();
		LinkedList<Diff> diff = dmp.diffMain(text1, text2, false);
		dmp.diffCleanupSemantic(diff);
		LinkedList<Patch> patch = dmp.patchMake(diff);
		return dmp.patchToText(patch);
	}

	private String applyPatch(String text, String patchText) {
		DiffMatchPatch dmp = new DiffMatchPatch();
		List<Patch> patch = dmp.patchFromText(patchText);
		Object[] patched = dmp.patchApply(new LinkedList<>(patch), text);
		return (String) patched[0];
	}

}
