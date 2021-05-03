package mine.imageweb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mine.imageweb.entity.Tag;

public interface TagRepository extends JpaRepository<Tag,Long>{
	Optional<Tag> findByName(String name);
	
//	@Query(value = "select * from tag where name=?1",nativeQuery = true)
//	public List<Tag> findByName(String name);
	
	@Query(value = "select be_tag.* from be_tag inner join be_post_tag on be_tag.id = be_post_tag.tag_id where be_post_tag.post_id =?1",nativeQuery = true)
	public List<Tag> findByPostID(Long postID);
	
	@Query(value = "insert into be_post_tag(post_id, tag_id) value(?1,?2)",nativeQuery = true)
	public void insertPostTag(Long postID, Long tagID);
}
