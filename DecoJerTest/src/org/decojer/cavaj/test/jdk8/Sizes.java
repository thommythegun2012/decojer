package org.decojer.cavaj.test.jdk8;

@java.lang.annotation.Documented
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {
		java.lang.annotation.ElementType.TYPE_USE,
		java.lang.annotation.ElementType.TYPE_PARAMETER })
public @interface Sizes {

	// is allowed, ignore...
	int muh() default 1;

	Size[] value();

}