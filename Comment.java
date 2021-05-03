package mine.imageweb.entity;


import javax.persistence.Column;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "be_comment")
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column
	private String content;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name="post_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Post post;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name="user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

}
