package org.decojer.cavaj.test.jdk8;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface Sizes {

	// is allowed, ignore...
	int muh() default 1;

	Size[] value();

}