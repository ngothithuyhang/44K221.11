package mine.imageweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import mine.imageweb.entity.Album;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long>{
	Optional<Album> findByName(String name);
	
	@Query(value = "select * from be_album where user_id = ?1",nativeQuery = true)
	List<Album> findByUserID(Long userID);
	
	
}
