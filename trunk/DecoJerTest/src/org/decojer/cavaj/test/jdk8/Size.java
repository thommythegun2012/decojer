package org.decojer.cavaj.test.jdk8;

import java.lang.annotation.Repeatable;

@java.lang.annotation.Documented
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {
		java.lang.annotation.ElementType.TYPE_USE,
		java.lang.annotation.ElementType.TYPE_PARAMETER })
@Repeatable(Sizes.class)
public @interface Size {

	int max();

	int min() default 0;

}