/**
 * 
 */
package com.fengyonggang.jpa.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author fengyonggang
 *
 */
public class PredicateUtils {

	public static <T> Predicate equal(Root<?> root, CriteriaBuilder cb, String field, T value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.equal(root.get(field), value);
				}
			} else {
				return cb.equal(root.get(field), value);
			}
		}
		return null;
	}
	
	public static Predicate like(Root<?> root, CriteriaBuilder cb, String field, String value) {
		if (value != null && !value.isEmpty()) {
			return cb.like(root.<String>get(field), "%" + value + "%");
		}
		return null;
	}
	
	public static Predicate likePrefix(Root<?> root, CriteriaBuilder cb, String field, String value) {
		if (value != null && !value.isEmpty()) {
			return cb.like(root.<String>get(field), "%" + value);
		}
		return null;
	}
	
	public static void like(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, String value) {
		Predicate predicate = like(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}
	
	public static void likePrefix(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, String value) {
		Predicate predicate = likePrefix(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}
	
	public static <T> Predicate existOrNot(Root<?> root, CriteriaBuilder cb, String field, T value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					if (((String) value).equalsIgnoreCase("Y")) {
						return cb.isNotNull(root.get(field));
					} else {
						return cb.isNull(root.get(field));
					}
				}
			} else if (value instanceof Boolean){
				if ((Boolean) value) {
					return cb.isNotNull(root.get(field));
				} else {
					return cb.isNull(root.get(field));
				}
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Predicate greaterThanOrEqualTo(Root<?> root, CriteriaBuilder cb, String field, Comparable<T> value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.greaterThanOrEqualTo(root.<Comparable>get(field), (Comparable) value);
				}
			} else {
				return cb.greaterThanOrEqualTo(root.<Comparable>get(field), (Comparable) value);
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Predicate greaterThan(Root<?> root, CriteriaBuilder cb, String field, Comparable<T> value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.greaterThan(root.<Comparable>get(field), (Comparable) value);
				}
			} else {
				return cb.greaterThan(root.<Comparable>get(field), (Comparable) value);
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Predicate lessThanOrEqualTo(Root<?> root, CriteriaBuilder cb, String field, Comparable<T> value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.lessThanOrEqualTo(root.<Comparable>get(field), (Comparable) value);
				}
			} else {
				return cb.lessThanOrEqualTo(root.<Comparable>get(field), (Comparable) value);
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Predicate lessThan(Root<?> root, CriteriaBuilder cb, String field, Comparable<T> value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.lessThan(root.<Comparable>get(field), (Comparable) value);
				}
			} else {
				return cb.lessThan(root.<Comparable>get(field), (Comparable) value);
			}
		}
		return null;
	}

	public static <T> void greaterThan(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, Comparable<T> value) {
		Predicate predicate = greaterThan(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	public static <T> void greaterThanOrEqualTo(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, Comparable<T> value) {
		Predicate predicate = greaterThanOrEqualTo(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	public static <T> void lessThan(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, Comparable<T> value) {
		Predicate predicate = lessThan(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	public static <T> void lessThanOrEqualTo(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, Comparable<T> value) {
		Predicate predicate = lessThanOrEqualTo(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	public static <T> void equal(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, T value) {
		Predicate predicate = equal(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	public static <T> void existOrNot(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, T value) {
		Predicate predicate = existOrNot(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	private static <T> void in(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, List<T> values) {
		Predicate predicate = in(root, cb, field, values);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	private static <T> Predicate in (Root<?> root, CriteriaBuilder cb, String field, List<T> values) {
		if(!CollectionUtils.isEmpty(values)) {
			Iterator<T> iterator = values.iterator();
			CriteriaBuilder.In<T> in = cb.in(root.get(field));
			while (iterator.hasNext()) {
				in.value(iterator.next());
			}
			return in;
		}
		return null;
	}

	private static void isNull(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field) {
		Predicate predicate = isNull(root, cb, field);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	private static Predicate isNull(Root<?> root, CriteriaBuilder cb, String field) {
		return cb.isNull(root.get(field));
	}

	private static <T> void isNullOrEq(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, T value) {
		Predicate predicate = isNullOrEq(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	private static <T> Predicate isNullOrEq(Root<?> root, CriteriaBuilder cb, String field, T value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.or(root.get(field).isNull(),cb.equal(root.get(field), value));
				}
			} else {
				return cb.or(root.get(field).isNull(),cb.equal(root.get(field), value));
			}
		}
		return null;
	}

	private static <T> void notEq(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, String field, T value) {
		Predicate predicate = notEq(root, cb, field, value);
		if (predicate != null) {
			predicates.add(predicate);
		}
	}

	private static <T> Predicate notEq(Root<?> root, CriteriaBuilder cb, String field, T value) {
		if (value != null) {
			if (value instanceof String) {
				if (!((String) value).isEmpty()) {
					return cb.notEqual(root.get(field), value);
				}
			} else {
				return cb.notEqual(root.get(field), value);
			}
		}
		return null;
	}

	public static <T> Predicate build(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder cb, T obj) {
		if (obj == null)
			return null;

		List<Predicate> predicates = new ArrayList<>();

		buildPredicate(root, query, cb, predicates, obj);

		Class<?> clazz = obj.getClass();
		
		Field [] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			Method method = getMethod(clazz, field);
			if (method == null) {
				continue;
			}
			buildPredicate(root, cb, predicates, field, ReflectionUtils.invokeMethod(method, obj));
		}
		return cb.and(predicates.toArray(new Predicate[predicates.size()]));
	}
	
	private static <T> void buildPredicate(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder cb, List<Predicate> predicates, T obj) {
		PredicateClass pc = obj.getClass().getAnnotation(PredicateClass.class);
		if (pc != null) {
			Class<? extends PredicateBuilder<?>> pbClass = pc.value();
			if (pbClass != null) {
				@SuppressWarnings("unchecked")
				PredicateBuilder<T> pb = (PredicateBuilder<T>) BeanUtils.instantiate(pbClass);
				List<Predicate> pds = pb.build(root, query, cb, obj);
				if (pds != null && pds.size() > 0) {
					predicates.addAll(pds);
				}
			}
		}
	}
	
	private static Method getMethod(Class<?> clazz, Field field) {
		String methodName = "get" + StringUtils.capitalize(field.getName());
		Method method = ReflectionUtils.findMethod(clazz, methodName);
		if (method == null && (field.getType() == Boolean.class || field.getType() == boolean.class)) {
			methodName = "is" + StringUtils.capitalize(field.getName());
			method = ReflectionUtils.findMethod(clazz, methodName);
		}
		return method;
	}

	private static <T> void buildPredicate(Root<?> root, CriteriaBuilder cb, List<Predicate> predicates, Field field, T value) {
		PredicateField ep = field.getAnnotation(PredicateField.class);
		if (ep == null) {
			equal(root, cb, predicates, field.getName(), value);
		} else {
			if (ep.ignore()) {
				return ;
			}
			
			String propertyName = StringUtils.hasLength(ep.propertyName()) ? ep.propertyName() : field.getName();
			Class<?> propertyType = ep.propertyType();
			switch (ep.operation()) {
			case eq:
				equal(root, cb, predicates, propertyName, value);
				break;
			case greaterThan:
				greaterThan(root, cb, predicates, propertyName, castToComparable(ep.propertyType(), value));
				break;
			case lessThan:
				lessThan(root, cb, predicates, propertyName, castToComparable(propertyType, value));
				break;
			case greaterThanOrEqualTo:
				greaterThanOrEqualTo(root, cb, predicates, propertyName, castToComparable(propertyType, value));
				break;
			case lessThanOrEqualTo:
				lessThanOrEqualTo(root, cb, predicates, propertyName, castToComparable(propertyType, value));
				break;
			case existOrNot:
				existOrNot(root, cb, predicates, propertyName, value);
				break;
			case like: 
				like(root, cb, predicates, propertyName, ((String) value));
				break;
			case likePrefix: 
				likePrefix(root, cb, predicates, propertyName, ((String) value));
				break;
			case in:
				in(root, cb, predicates, propertyName, ((List<?>) value));
				break;
			case isNull:
				isNull(root, cb, predicates, propertyName);
				break;
			case isNullOrEq:
				isNullOrEq(root, cb, predicates, propertyName, value);
				break;
			case notEq:
				notEq(root, cb, predicates, propertyName, value);
				break;
			default:
				equal(root, cb, predicates, propertyName, value);
				break;
			}
		}
	}

	private static <T> Comparable<?> castToComparable(Class<?> propertyType, T value) {
		if (value == null) {
			return null;
		}
		if (propertyType != Object.class && propertyType != value.getClass()) {
			if (propertyType == Date.class) {
				Date dateValue = parseSimpleDate(value.toString());
				if (dateValue == null)
					throw new RuntimeException("invalid date value: " + value);
				return Comparable.class.cast(dateValue);
			}
			throw new RuntimeException("invalid type for value: " + value);
		} else {
			return Comparable.class.cast(value);
		}
	}
	
	private static Date parseSimpleDate(String simpleDate) {
		try {			
			// TODO make the pattern configurable
			return new SimpleDateFormat("yyyy-MM-dd").parse(simpleDate);
		} catch (Exception e) {
			return null;
		}
	}
	
}
