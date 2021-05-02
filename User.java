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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.hibernate.annotations.NaturalId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Entity
@Table(name = "be_users", uniqueConstraints = { @UniqueConstraint(columnNames = { "email" }) })

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User extends DateAudit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 20)
	private String username;

	@NaturalId
	@NotBlank
	@Size(max = 40)
	@Email
	private String email;

	@NotBlank
	@Size(max = 20)
	private String password;

	@Column(columnDefinition = "varchar(50) default null")
	private String instagram;
	@Column(columnDefinition = "varchar(50) default null")
	private String facebook;
	@Column(columnDefinition = "varchar(50) default null")
	private String address;
	@Column(columnDefinition = "varchar(11) default null")
	private String phone;
	@Column(columnDefinition = "varchar(100) default null")
	private String aboutMe;
	@Column
	private String avatar;
	@Column
	private String background;

	public User(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "be_user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@OneToMany(mappedBy = "user")
	private Set<Album> albums = new HashSet<Album>();

	@OneToMany(mappedBy = "user")
	private Set<Post> post = new HashSet<Post>();
	
	@OneToMany(mappedBy = "user")
	private Set<Follow> follower = new HashSet<Follow>();

	@OneToMany(mappedBy = "user")
	private Set<Comment> comment = new HashSet<Comment>();
	
	@OneToMany(mappedBy = "user")
	private Set<Notify> notify = new HashSet<Notify>();
	
	@OneToMany(mappedBy = "user")
	private Set<LikeCount> likeCount = new HashSet<LikeCount>();

}
