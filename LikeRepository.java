package mine.imageweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mine.imageweb.entity.LikeCount;

public interface LikeRepository extends JpaRepository<LikeCount,Long>{

	@Query(value="select * from be_like_count where post_id=?1 and user_id=?2",nativeQuery=true)
	public List<LikeCount> findByPostID_UserID(Long postID, Long userID);
}
