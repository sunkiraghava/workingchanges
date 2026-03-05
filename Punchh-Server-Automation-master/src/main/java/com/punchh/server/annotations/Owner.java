package com.punchh.server.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // keep it at runtime so listener can access it
@Target({ ElementType.METHOD }) // applies only on methods (like @Test)
public @interface Owner {
	String name();
}