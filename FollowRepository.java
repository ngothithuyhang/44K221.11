package mine.imageweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mine.imageweb.entity.Follow;

public interface FollowRepository extends JpaRepository<Follow,Long>{

	@Query(value="select * from be_follow where post_id=?1 and user_id=?2",nativeQuery=true)
    List<Follow> findByPostID_UserID(Long postID, Long userID);
	
	@Query(value="select * from be_follow where post_id=?1",nativeQuery=true)
    public List<Follow>findByPostID(Long postID);
}
