package mine.imageweb.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import mine.imageweb.entity.Role;
import mine.imageweb.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);


    List<User> findByIdIn(List<Long> userIds);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
    
    @Query(value="select be_users.* from be_users inner join be_user_roles on be_users.id=be_user_roles.user_id where be_user_roles.role_id=?1",nativeQuery=true)
    List<User> findByRoleID(Long roleID);
    
}