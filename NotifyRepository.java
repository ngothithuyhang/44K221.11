package mine.imageweb.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mine.imageweb.entity.Notify;

public interface NotifyRepository extends JpaRepository<Notify,Long>{
	@Query(value="select * from be_notify where user_id=?1 order by id desc limit ?2",nativeQuery=true)
    public List<Notify>findByUserID(Long userID, int limit);
	
	@Query(value="select * from be_notify where user_id=?1 and status=?2 order by id desc limit ?3",nativeQuery=true)
    public List<Notify>findByUserIDStatus(Long userID, int status, int limit);
}
