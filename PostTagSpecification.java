package mine.imageweb.repository.specification;

import java.util.Collection;

import javax.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import mine.imageweb.entity.PostTag;
import mine.imageweb.entity.Tag;

public final class PostTagSpecification {
	public static Specification<PostTag> hasId(long postID) {
		return (root, query, cb) -> cb.equal(root.get("id"), postID);
	}


	public static Specification<PostTag> hasIdIn(Collection<Long> listID) {
		return (root, query, cb) -> root.get("id").in(listID);
	}
	
	public static Specification<PostTag> hasTagIn(Collection<Tag> listTag) {
		return (root, query, cb) -> root.get("tag").in(listTag);
	}
}
