package mine.imageweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import mine.imageweb.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
	@Query(value = "select * from be_post where user_id=?1",nativeQuery = true)
	public List<Post> findByUserID(Long userID);

	@Query(value = "select * from be_post where title like ?1 order by id desc limit ?2", nativeQuery = true)
	public List<Post> findTop(String key,  int limit);
	
	@Query(value = "select * from be_post order by id desc limit 1", nativeQuery = true)
	public List<Post> findLastest();
}
