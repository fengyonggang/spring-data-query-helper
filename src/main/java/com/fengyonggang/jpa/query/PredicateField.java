/**
 * 
 */
package com.fengyonggang.jpa.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fengyonggang
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PredicateField {

	String propertyName() default "";

	QueryOperation operation() default QueryOperation.eq;
	
	Class<?> propertyType() default Object.class;
	
	boolean ignore() default false;
	
}
