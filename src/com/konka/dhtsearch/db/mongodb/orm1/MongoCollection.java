package com.konka.dhtsearch.db.mongodb.orm1;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MongoCollection 
{
	String value() default "";
}


