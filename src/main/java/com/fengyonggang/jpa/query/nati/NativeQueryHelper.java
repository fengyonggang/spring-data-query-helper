/**
 * 
 */
package com.fengyonggang.jpa.query.nati;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.From;
import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.Join;
import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.JoinMeta;
import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.ParamValuePair;
import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.Where;

/**
 * @author fengyonggang
 *
 */
public class NativeQueryHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(NativeQueryHelper.class);
	
	private EntityManager entityManager;
	
	private static final String SPACE = " ";
	
	private static final List<String> INGORE_ORDER_WORDS = Arrays.asList("order","by","asc","desc");
	
	public NativeQueryHelper(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public <T> Page<T> query(NativeQueryMetadata metadata, Pageable pageable) {
		long count = queryCount(metadata);
		
		List<T> list = null;
		if (count > 0) {
			list = queryList(metadata, pageable);
		} else {
			list = new ArrayList<>();
		}
		return new PageImpl<>(list, pageable, count);
	}
	
	public <T> List<T> query(NativeQueryMetadata metadata, int limit, Sort sort) {
		return queryList(metadata, sort, 0, limit);
	}
	
	public <T> List<T> query(NativeQueryMetadata metadata) {
		return queryList(metadata);
	}
	
	public <T> T findOne(NativeQueryMetadata metadata) {
		List<T> list = queryList(metadata);
		if (list != null && list.size() > 0) {
			return list.get(0);
		}
		return null;
	}
	
	public String getQuery(NativeQueryMetadata metadata) {
		return this.buildSql(metadata);
	}
	
	public String getCountQuery(NativeQueryMetadata metadata) {
		return this.buildCountSql(metadata);
	}
	
	private <T> List<T> queryList(NativeQueryMetadata metadata) {
		return queryList(metadata, null);
	}
	
	private <T> List<T> queryList(NativeQueryMetadata metadata, Pageable pageable) {
		int offset = 0;
		int pageSize = Integer.MAX_VALUE;
		
		if (pageable != null) {
			offset = pageable.getOffset();
			pageSize = pageable.getPageSize();
		} else if (metadata.getLimit() != null && metadata.getLimit() > 0) {
			pageSize = metadata.getLimit();
		}
		return queryList(metadata, pageable == null ? null : pageable.getSort(), offset, pageSize);
	}
	
	private <T> List<T> queryList(NativeQueryMetadata metadata, Sort sort, int offset, int pageSize) {
		String sql = buildSql(metadata, sort);
		LOGGER.debug("sql to be executed: {}", sql);
		
		Query query = entityManager.createNativeQuery(sql);
		setParameter(query, metadata);
		query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		
		 query.setFirstResult(offset);
		 query.setMaxResults(pageSize);
		
		List<?> result = query.getResultList();
		
		return handleResult(result, metadata.getResultClass());
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> handleResult(List<?> result, Class<?> resultType) {
		List<T> mapperedResult = new ArrayList<>();
		if (!result.isEmpty()) {
			if (resultType == null && ((Map<String, Object>) result.get(0)).size() == 1) {
				// just one field 
				for (Object rowObj : result) {
					Map<String, Object> row = (Map<String, Object>) rowObj;
					for (Object value : row.values()) {
						mapperedResult.add((T) value);
					}
				}
			} else {
				Assert.notNull(resultType, "result class should not be null");
				for (Object rowObj : result) {
					Map<String, Object> row = (Map<String, Object>) rowObj;
					T mapperedRow = (T) BeanUtils.instantiate(resultType);
					for(Entry<String, Object> entry : row.entrySet()) {
						Field field = findField(resultType, entry.getKey());
						if (field == null) {
							LOGGER.warn("can not find field {} from class {}", entry.getKey(), resultType);
						} else {
							ReflectionUtils.makeAccessible(field);
							ReflectionUtils.setField(field, mapperedRow, convertValue(entry.getValue(), field.getType()));
						}
					}
					mapperedResult.add(mapperedRow);
				}
			}
		} 
		return mapperedResult;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object convertValue(Object value, Class<?> expectedType) {
		if (value == null) {
			return null;
		}
		
		if (value instanceof Number && Number.class.isAssignableFrom(expectedType)) {
			return NumberUtils.convertNumberToTargetClass((Number) value, (Class<? extends Number>) expectedType);
		}
		
		if (String.class.isAssignableFrom(expectedType)) {
			value = value.toString();
		}
		
		if (Enum.class.isAssignableFrom(expectedType)) {
			value = Enum.valueOf((Class<? extends Enum>) expectedType, value.toString());
		}
		
		return value;
	}
	
	private Field findField(Class<?> clazz, String fieldName) {
		List<String> fieldNames = new ArrayList<>();
		if (fieldName.indexOf('_') != -1) {
			StringBuilder fieldBuilder = new StringBuilder();
			for (String seg : fieldName.split("_")) {
				if (fieldBuilder.length() > 0) {
					fieldBuilder.append(StringUtils.capitalize(seg));
				} else {
					fieldBuilder.append(seg);
				}
			}
			fieldNames.add(fieldBuilder.toString());
		}
		fieldNames.add(fieldName);
		
		for (String fn : fieldNames) {
			Field field = ReflectionUtils.findField(clazz, fn);
			if (field != null) {
				return field;
			}
		}
		
		return null;
	}

	private long queryCount(NativeQueryMetadata metadata) {
		String sql = buildCountSql(metadata);
		LOGGER.debug("sql to be executed: {}", sql);
		Query query = entityManager.createNativeQuery(sql);
		setParameter(query, metadata);
		return ((Number) query.getSingleResult()).longValue();
	}

	private String buildCountSql(NativeQueryMetadata metadata) {
		return "select count(1) from (" + buildSql(metadata, null, true) + ") t";
	}
	
	private String buildSql(NativeQueryMetadata metadata) {
		return buildSql(metadata, null);
	}
	
	private String buildSql(NativeQueryMetadata metadata, Sort sort) {
		return buildSql(metadata, sort, false);
	}
	
	private String buildSql(NativeQueryMetadata metadata, Sort sort, boolean count) {
		StringBuilder builder = new StringBuilder();
		
		appendAndCheckStartWord(builder, metadata.getSelect(), "select");
		
		if (metadata.getSubQuery() != null) {
			buildFrom(builder, metadata.getSubQuery());
		} else {
			appendAndCheckStartWord(builder, metadata.getFrom(), "from");
		}
		
		for (Join join : metadata.getJoins()) {
			if (join.isJoin()) {
				if (join.getJoinMeta() != null) {
					buildJoin(builder, join.getJoinMeta());
				} else {
					builder.append(join.getJoin()).append(SPACE);
				}
			}
		}
		
		builder.append(buildWhere(metadata.getWhere())).append(SPACE);
		
		appendAndCheckStartWord(builder, metadata.getGroupby(), "group by");
		
		if (!count) { // for count sql, no need order by
			builder.append(buildOrderBy(metadata.getOrderBys(), sort));
		}
		
		return builder.toString();
	}
	
	private void buildFrom(StringBuilder builder, From from) {
		String fromQuery = buildSql(from.getFromMeta());
		builder.append("from (").append(fromQuery).append(")").append(SPACE).append(from.getAlias()).append(SPACE);
	}
	
	private void buildJoin(StringBuilder builder, JoinMeta joinMeta) {
		String fromQuery = buildSql(joinMeta.getMeta());
		builder.append(joinMeta.getJoinKeyWord()).append(" (").append(fromQuery).append(")").append(SPACE)
				.append(joinMeta.getAlias()).append(SPACE).append("on").append(SPACE).append(joinMeta.getJoinCondition()).append(SPACE);
	}
	
	private void appendAndCheckStartWord(StringBuilder builder, String str, String startWord) {
		if (StringUtils.hasLength(str)) {
			if (str.trim().toLowerCase().startsWith(startWord)) {
				builder.append(str).append(SPACE);
			} else {
				builder.append(startWord).append(SPACE).append(str).append(SPACE);
			}
		}
	}
	
	/** 
	 * 1. 如果不存在sort，  用orderBys来排序 <br/>
	 * 2. 如果存在sort， 不存在orderBys， 用sort来排序 <br/>
	 * 3. 如果存在sort， 同时存在orderBys， 用sort来排序，但是排序字段用orderBys中的来替换。 例如： <br/> 
	 * 	  sort:  orderId asc, settlePrice desc  <br/>
	 *    orderBys:  i.order_id desc, i.settle_price asc  <br/>
	 *    最终的排序是: i.order_id asc, i.settle_price desc  <br/>
	 * @param orderBys
	 * @param sort
	 * @return
	 */
	private String buildOrderBy(List<String> orderBys, Sort sort) {
		List<String> actualOrderBys = null;
		if (sort == null) {
			actualOrderBys = orderBys;
		} else {
			actualOrderBys = extractOrder(sort, orderBys);
		}
		
		StringBuilder orderByBuilder = new StringBuilder();
		if (actualOrderBys != null) {
			for (String orderby : actualOrderBys) {
				if (orderByBuilder.length() > 0) {
					orderByBuilder.append(',').append(SPACE);
				}
				orderByBuilder.append(orderby).append(SPACE);
			}
		}
		return orderByBuilder.length() > 0 ? "order by " + orderByBuilder : "";
	}

	private List<String> extractOrder(Sort sort, List<String> orderBys) {
		Map<String, String> realFieldMap = populateOrderByFields(orderBys);
		List<String> result = new ArrayList<>();
		if (sort != null) {
			for (Order order : sort) {
				boolean found = false;
				if (realFieldMap != null && realFieldMap.size() > 0) {
					for (String property : extractOrderProperty(order.getProperty())) {
						if (realFieldMap.containsKey(property)) {
							result.add(realFieldMap.get(property) + SPACE + order.getDirection());
							found = true;
							break;
						}
					}
				}
				if (!found) {
					result.add(order.getProperty() + SPACE + order.getDirection());
				}
			}
		}
		return result;
	}
	
	private List<String> extractOrderProperty(String property) {
		List<String> result = new ArrayList<>();
		
		StringBuilder builder = new StringBuilder();
		for (char c : property.toCharArray()) {
			if (Character.isUpperCase(c)) {
				builder.append('_').append(Character.toLowerCase(c));
			} else {
				builder.append(c);
			}
		}
		result.add(builder.toString());
		
		if (!result.contains(property)) {
			result.add(property);
		}
		
		return result;
	}
	
	private Map<String, String> populateOrderByFields(List<String> orderBys) {
		if (orderBys == null) {
			return null;
		}

		Map<String, String> fieldMap = new HashMap<>();
		for (String orderBy : orderBys) {
			for (String singleOrderBy : orderBy.split(",")) {
				for (String word : singleOrderBy.split("\\s+")) {
					if (!INGORE_ORDER_WORDS.contains(word.toLowerCase())) {
						fieldMap.put(word.indexOf(".") == -1 ? word : word.substring(word.indexOf(".") + 1), word);
						break;
					}
				}
			}
		}
		return fieldMap;
	}
	
	private String buildWhere(Where where) {
		StringBuilder builder = new StringBuilder();
		if (where.getStatements() != null) {
			for (String statement : where.getStatements()) {
				if (builder.length() > 0) {
					builder.append(" and ");
				}
				builder.append(statement);
			}
		}
		
		if (where.getPairs() != null) {
			for (ParamValuePair pair : where.getPairs()) {
				if (builder.length() > 0) {
					builder.append(" and ");
				}
				builder.append(pair.getParam());
			}
		}
		return builder.length() == 0 ? "" : ("where " + builder.toString());
	}
	
	public void setParameter(Query query, NativeQueryMetadata meta) {
		int position = 1;
		if (meta.getSubQuery() != null) {
			Where where = meta.getSubQuery().getFromMeta().getWhere();
			position = setParameter(query, where, position);
		}
		
		for (Join join : meta.getJoins()) {
			if (join.getJoinMeta() != null && join.isJoin()) {
				Where where = join.getJoinMeta().getMeta().getWhere();
				position = setParameter(query, where, position);
			}
		}
		
		position = setParameter(query, meta.getWhere(), position);
	}
	
	private int setParameter(Query query, Where where, int position) {
		if (where.getPairs() == null || where.getPairs().isEmpty()) {
			return position;
		}
		
		for (ParamValuePair pair : where.getPairs()) {
			if (pair.getValue() instanceof Collection) {
				for (Object v : (Collection<?>) pair.getValue()) {
					query.setParameter(position ++, v);
				}
			} else {
				query.setParameter(position ++, pair.getValue());
			}
		}
		LOGGER.debug("sql parameters: {}", where.getPairs());
		return position;
	}
	
}
