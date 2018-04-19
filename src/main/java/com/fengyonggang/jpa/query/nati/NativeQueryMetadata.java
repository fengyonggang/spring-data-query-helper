package com.fengyonggang.jpa.query.nati;
/**
 * 
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author fengyonggang
 *
 */
@Data
public class NativeQueryMetadata {

	private String select; 
	private String from; 
	private List<Join> joins = new ArrayList<>();
	private List<String> orderBys = new ArrayList<>();
	private String groupby;
	private List<ParamValuePair> where = new ArrayList<>();
	private Class<?> resultClass;
	
	private static final Pattern IN_PATTERN = Pattern.compile("in\\s+\\?", Pattern.CASE_INSENSITIVE);
	
	public void addWhere(ParamValuePair pair) {
		this.where.add(pair);
	}
	
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
			}
			this.addWhere(new ParamValuePair(param, value));
		}
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
	public static class Join {
		private String join;
		private List<Object> params;
		
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
	@AllArgsConstructor
	public static class ParamValuePair {
		private String param;
		private Object value;
	}
}
