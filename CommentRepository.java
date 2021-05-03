package mine.imageweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mine.imageweb.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long>{

	 @Query(value="select * from be_comment where post_id=?1",nativeQuery=true)
	List<Comment> findByPostID(Long postID);
}
