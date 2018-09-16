package com.fengyonggang.jpa.query;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * @author fengyonggang
 *
 */
public interface PredicateBuilder<T> {

	List<Predicate> build(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder cb, T t);
}
