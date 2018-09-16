/**
 * 
 */
package com.fengyonggang.jpa.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * @author fengyonggang
 *
 */
public class SpecificationUtils {

	public static <T, P> Specification<T> build(final P param) {
		return new Specification<T>() {
			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return PredicateUtils.build(root, query, cb, param);
			}
		};
	}
	
	public static <T> Specification<T> build(final Map<String, Object> params) {
		if (params == null || params.isEmpty()) 
			return null;
		
		return new Specification<T>() {
			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> predicates = new ArrayList<>();
				for (Entry<String, Object> param : params.entrySet()) {
					if (StringUtils.isEmpty(param.getKey()) || param.getValue() == null) {
						continue;
					}
					if (param.getValue() instanceof String && ((String) param.getValue()).isEmpty()) {
						continue;
					}
					PredicateUtils.equal(root, cb, predicates, param.getKey(), param.getValue());
				}
				return cb.and(predicates.toArray(new Predicate[predicates.size()]));
			}
		};
	}
	
	public static void buildMapParams(Map<String, Object> params, String key, Object value) {
		if (!StringUtils.isEmpty(key) && value != null) {
			params.put(key, value);
		}
	}
}
