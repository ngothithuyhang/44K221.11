package mine.imageweb.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mine.imageweb.entity.Comment;
import mine.imageweb.entity.User;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CommentObject {
	private User user;
	private Comment comment;
}
