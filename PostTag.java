package mine.imageweb.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "be_post_tag")
public class PostTag {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name="post_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Post post;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name="tag_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	public Tag tag;
}
