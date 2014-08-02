package com.eaybars.beans.index;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
@Inherited
public @interface Index {
    
    boolean contents() default false;
    boolean sorted() default false;
    @SuppressWarnings("rawtypes")
    Class<? extends Comparator> comparator() default Comparator.class;

}
