package mine.imageweb.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "be_post")
//@Builder
public class Post extends DateAudit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String title;

	@Column
	private String content;

	@Column
	private Long price;
	
	@Column
	private int status;

	@OneToMany(mappedBy = "post")
	private Set<Image> images = new HashSet<Image>();
	
	@OneToMany(mappedBy = "post")
	private Set<Comment> comment = new HashSet<Comment>();
	
	@OneToMany(mappedBy = "post")
	private Set<Follow> follower = new HashSet<Follow>();
	
	@OneToMany(mappedBy = "post")
	private Set<Notify> notify = new HashSet<Notify>();

	@OneToMany(mappedBy = "post")
	private Set<LikeCount> likeCount = new HashSet<LikeCount>();
	
	@OneToMany(mappedBy = "post")
	private Set<PostTag> postTag = new HashSet<PostTag>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;
	

	public Post(String title, String content, Long price, Set<Image> images, User user) {
		super();
		this.title = title;
		this.content = content;
		this.price = price;
		this.images = images;
		this.user = user;
		this.status=0;
	}

}
