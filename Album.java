package mine.imageweb.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "be_album")
public class Album {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column
	@NotNull
	private String name;
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private User user;
	
	@OneToMany(mappedBy = "album")
	private Set<Image> images = new HashSet<Image>();
	
//	@OneToMany(mappedBy = "image")
//	private Set<Comment> comment = new HashSet<Comment>();

	public Album(@NotNull String name, User user) {
		super();
		this.name = name;
		this.user = user;
	}
	
	
}
