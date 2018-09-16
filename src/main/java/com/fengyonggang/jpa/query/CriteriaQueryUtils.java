package com.fengyonggang.jpa.query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

public class CriteriaQueryUtils {

	/**
	 * 
	 * 借助于 SpecificationUtils 动态构建 CriteriaQuery
	 * 
	 * @param builder, 从EntityManager中获取：  entityManager.getCriteriaBuilder()
	 * @param param 参数对象
	 * @param entityType 实体bean的class
	 * @param returnType 返回类型
	 * @param callback 查询内容回调函数
	 * @return
	 */
	public static <R, P, E> CriteriaQuery<R> build(CriteriaBuilder builder, final P param, Class<E> entityType,
			Class<R> returnType, SelectionCallback<R, E> callback) {
		
		CriteriaQuery<R> query = builder.createQuery(returnType);

		Root<E> root = query.from(entityType);

		Specification<E> spec = SpecificationUtils.build(param);
		if (spec != null) {
			Predicate predicate = spec.toPredicate(root, query, builder);
			if (predicate != null) {
				query.where(predicate);
			}
		}

		callback.select(query, root);

		return query;
	}
	
	public static interface SelectionCallback<R, E> {
		void select(CriteriaQuery<R> query, Root<E> root);
	}
}
