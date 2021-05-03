package mine.imageweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import mine.imageweb.entity.PostTag;

public interface PostTagRepository extends JpaRepository<PostTag,Long>, 
				JpaSpecificationExecutor<PostTag>{
//				PagingAndSortingRepository<PostTag, Long>{

}
