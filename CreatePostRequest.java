package mine.imageweb.request;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreatePostRequest {
	private String title;
	private Long albumID;
	private String tag;
	private String content;
	private MultipartFile[] files;
	private Long price;
}
