/**
 * 
 */
package com.fengyonggang.jpa.query.nati;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.Join;
import com.fengyonggang.jpa.query.nati.NativeQueryMetadata.ParamValuePair;

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
	
	public <T> List<T> query(NativeQueryMetadata metadata) {
		return queryList(metadata);
	}
	
	private <T> List<T> queryList(NativeQueryMetadata metadata) {
		return queryList(metadata, null);
	}
	
	private <T> List<T> queryList(NativeQueryMetadata metadata, Pageable pageable) {
		String sql = buildSql(metadata, pageable == null ? null : pageable.getSort());
		LOGGER.debug("sql to be executed: {}", sql);
		
		Query query = entityManager.createNativeQuery(sql);
		setParameter(query, metadata.getWhere());
		query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		if (pageable != null) {
			query.setFirstResult(pageable.getOffset());
			query.setMaxResults(pageable.getPageSize());
		}
		
		List<?> result = query.getResultList();
		
		List<T> mapperedResult = new ArrayList<>();
		
		if (!result.isEmpty()) {
			Assert.notNull(metadata.getResultClass(), "result class should not be null");
			
			for (Object rowObj : result) {
				@SuppressWarnings("unchecked")
				Map<String, Object> row = (Map<String, Object>) rowObj;
				@SuppressWarnings("unchecked")
				T mapperedRow = (T) BeanUtils.instantiate(metadata.getResultClass());
				for(Entry<String, Object> entry : row.entrySet()) {
					Field field = findField(metadata.getResultClass(), entry.getKey());
					if (field == null) {
						LOGGER.warn("can not find field {} from class {}", entry.getKey(), metadata.getResultClass());
					} else {
						ReflectionUtils.makeAccessible(field);
						ReflectionUtils.setField(field, mapperedRow, convertValue(entry.getValue()));
					}
				}
				mapperedResult.add(mapperedRow);
			}
		} 
		return mapperedResult;
	}
	
	private Object convertValue(Object value) {
		if (value instanceof BigInteger) {
			value = ((BigInteger) value).longValue();
		} else if (value instanceof Character) {
			value = value.toString();
		} else if (value instanceof BigDecimal) {
			value = ((BigDecimal) value).doubleValue();
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
		setParameter(query, metadata.getWhere());
		return ((Number) query.getSingleResult()).longValue();
	}

	private String buildCountSql(NativeQueryMetadata metadata) {
		return "select count(1) from (" + buildSql(metadata, null, true) + ") t";
	}
	
	private String buildSql(NativeQueryMetadata metadata, Sort sort) {
		return buildSql(metadata, sort, false);
	}
	
	private String buildSql(NativeQueryMetadata metadata, Sort sort, boolean count) {
		StringBuilder builder = new StringBuilder();
		
		appendAndCheckStartWord(builder, metadata.getSelect(), "select");
		appendAndCheckStartWord(builder, metadata.getFrom(), "from");
		
		for (Join join : metadata.getJoins()) {
			if (join.isJoin()) {
				builder.append(join.getJoin()).append(SPACE);
			}
		}
		
		builder.append(buildWhere(metadata.getWhere())).append(SPACE);
		
		appendAndCheckStartWord(builder, metadata.getGroupby(), "group by");
		
		if (!count) { // for count sql, no need order by
			builder.append(buildOrderBy(metadata.getOrderBys(), sort));
		}
		
		return builder.toString();
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
	
	private String buildWhere(List<ParamValuePair> pairs) {
		if (pairs == null || pairs.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for (ParamValuePair pair : pairs) {
			if (builder.length() > 0) {
				builder.append(" and ");
			}
			builder.append(pair.getParam());
		}
		return "where " + builder.toString();
	}
	
	private void setParameter(Query query, List<ParamValuePair> pairs) {
		if (pairs == null || pairs.isEmpty()) {
			return ;
		}
		int position = 1;
		for (ParamValuePair pair : pairs) {
			if (pair.getValue() instanceof Collection) {
				for (Object v : (Collection<?>) pair.getValue()) {
					query.setParameter(position ++, v);
				}
			} else {
				query.setParameter(position ++, pair.getValue());
			}
		}
		LOGGER.debug("sql parameters: {}", pairs);
	}
}
