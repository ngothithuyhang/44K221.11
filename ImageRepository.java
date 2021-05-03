package mine.imageweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import mine.imageweb.entity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long>{
	@Query(value="select * from be_images where post_id=?1",nativeQuery=true)
    List<Image> findByPostID(Long id);
}
