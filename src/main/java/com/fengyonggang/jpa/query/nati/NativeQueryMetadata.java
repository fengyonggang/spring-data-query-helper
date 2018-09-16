/**
 * 
 */
package com.fengyonggang.jpa.query.nati;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

/**
 * @author fengyonggang
 *
 */
@Data
public class NativeQueryMetadata {

	private String select; 
	private String from; 
	@Setter(AccessLevel.PRIVATE)
	private From subQuery;
	private List<Join> joins = new ArrayList<>();
	private List<String> orderBys = new ArrayList<>();
	private String groupby;
	private Where where = new Where(); 
	private Class<?> resultClass;
	private Integer limit;
	
	private static final Pattern IN_PATTERN = Pattern.compile("in\\s+\\?", Pattern.CASE_INSENSITIVE);
	
	public void addWhere(String param, Object value) {
		if (checkValue(value)) {
			if (value instanceof Collection && IN_PATTERN.matcher(param).find()) {
				param = param.replace("?", "(" + buildPlaceHolder(((Collection<?>) value).size()) + ")");
			} else if (param.toLowerCase().indexOf("like") != -1 && param.indexOf("%") != -1) {
				String stringValue = value.toString();
				if (param.indexOf("%?") != -1) {
					stringValue = "%" + stringValue;
					param = param.replace("%?", "?");
				}
				if (param.indexOf("?%") != -1) {
					stringValue = stringValue + "%";
					param = param.replace("?%", "?");
				}
				value = stringValue;
			} else if (value instanceof Enum) {
				value = ((Enum<?>) value).name();
			}
			this.where.addWhere(new ParamValuePair(param, value));
		}
	}
	
	public void setFrom(String from) {
		this.from = from;
	}
	
	public void setFrom(NativeQueryMetadata from, String alias) {
		this.subQuery = new From(from, alias);
	}
	
	public void addWhere(String statement) {
		this.where.addWhere(statement);
	}
	
	private String buildPlaceHolder(int size) {
		String[] arr = new String[size];
		Arrays.fill(arr, "?");
		return String.join(",", arr);
	}
	
	public void addJoin(String join, Object ... params) {
		if (params == null || params.length == 0) {
			this.joins.add(new Join(join));
		} else {
			this.joins.add(new Join(join, Arrays.asList(params)));
		}
	}
	
	public void addJoin(String keyWord, NativeQueryMetadata join, String alias, String joinCondition, Object ... params) {
		if (params == null || params.length == 0) {
			this.joins.add(new Join(new JoinMeta(keyWord, join, alias, joinCondition)));
		} else {
			this.joins.add(new Join(new JoinMeta(keyWord, join, alias, joinCondition), Arrays.asList(params)));
		}
	}
	
	public void addOrderBy(String orderby) {
		this.orderBys.add(orderby);
	}
	
	private static boolean checkValue(Object value) {
		if (value != null) {
			if (value instanceof String) {
				if (StringUtils.hasLength(value.toString())) {
					return true;
				}
			} else if (value instanceof Collection){
				if (((Collection<?>) value).size() > 0) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}
	
	@Data
	@AllArgsConstructor
	public static class From {
		private NativeQueryMetadata fromMeta;
		private String alias;
	}
	
	@Data
	@AllArgsConstructor
	public static class JoinMeta {
		private String joinKeyWord;
		private NativeQueryMetadata meta;
		private String alias;
		private String joinCondition;
	}
	
	@Data
	public static class Join {
		private String join;
		private JoinMeta joinMeta;
		private List<Object> params;
		
		public Join(JoinMeta joinMeta) {
			this.joinMeta = joinMeta;
			this.params = Arrays.asList(true);
		}
		
		public Join(JoinMeta joinMeta, List<Object> params) {
			this.joinMeta = joinMeta;
			this.params = params;
		}
		
		public Join(String join) {
			this.join = join;
			this.params = Arrays.asList(true);
		}
		
		public Join(String join, List<Object> params) {
			this.join = join;
			this.params = params;
		}
		
		public boolean isJoin() {
			if (params != null && params.size() > 0) {
				for (Object value : params) {
					if (checkValue(value)) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
	@Data
	public static class Where {
		private List<String> statements = new ArrayList<>(); 
		private List<ParamValuePair> pairs = new ArrayList<>();
		
		public void addWhere(ParamValuePair pair) {
			this.pairs.add(pair);
		}
		
		public void addWhere(String statement) {
			this.statements.add(statement);
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class ParamValuePair {
		private String param;
		private Object value;
	}
}
