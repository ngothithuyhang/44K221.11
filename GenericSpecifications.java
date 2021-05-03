package mine.imageweb.repository.specification;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

public class GenericSpecifications<T> {
	@SuppressWarnings("serial")
	public Specification<T> groupBy(Specification<T> specification, List<String> columnNames) {
	    return new Specification<T>() {

	        @Override
	        public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

	            List<Expression<?>> columnNamesExpression = columnNames.stream().map(x -> root.get(x))
	                    .collect(Collectors.toList());

	            query.groupBy(columnNamesExpression);
	            return specification.toPredicate(root, query, criteriaBuilder);
	        }
	    };
	}
}
