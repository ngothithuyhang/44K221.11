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
import javax.validation.constraints.NotNull;

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
@Table(name = "be_images")
public class Image {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column
	@NotNull
	private String name;
	
	@Column
	@NotNull
	private String link;
	
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name="album_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Album album;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name="post_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Post post;

	public Image(@NotNull String name,@NotNull String link, Album album, Post post) {
		super();
		this.name=name;
		this.link = link;
		this.album = album;
		this.post = post;
	}
	
	
}
