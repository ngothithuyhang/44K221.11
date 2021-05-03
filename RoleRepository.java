package mine.imageweb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import mine.imageweb.entity.Role;
import mine.imageweb.entity.RoleName;


@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName roleName);
    
    @Query(value="select be_roles.* from be_roles inner join be_user_roles on be_roles.id=be_user_roles.role_id where be_user_roles.user_id=?1",nativeQuery=true)
    List<Role> findByUserID(Long id);
}