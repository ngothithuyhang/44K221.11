package mine.imageweb.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeInfoRequest {
	private Long id;
	private String username;
	private String instagram;
	private String facebook;
	private String address;
	private String phone;
	private String email;
	private String password;
	private String newPassword;
	private String aboutMe;
	private String joinDate;
	private String avatar;
	private String background;

	public ChangeInfoRequest(Long id, String username, String instagram, String facebook, String address, String phone,
			String email, String password, String aboutMe, String joinDate, String avatar, String background) {
		this.id=id;
		this.username = username;
		this.instagram = instagram;
		this.facebook = facebook;
		this.address = address;
		this.phone = phone;
		this.email = email;
		this.password = password;
		this.aboutMe = aboutMe;
		this.joinDate = joinDate;
		this.avatar=avatar;
		this.background=background;
	}

}
