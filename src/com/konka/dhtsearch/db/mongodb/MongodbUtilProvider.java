package com.konka.dhtsearch.db.mongodb;

import java.net.UnknownHostException;

import com.konka.dhtsearch.db.mongodb.orm.MongodbUtil;
import com.mongodb.DB;
import com.mongodb.Mongo;

public class MongodbUtilProvider {
	public static MongodbUtil mongodbUtil;

	public static MongodbUtil getMongodbUtil() {

		if (mongodbUtil == null) {
			try {
				Mongo m = new Mongo("localhost", 27017);
				DB db = m.getDB("test");
				mongodbUtil = new MongodbUtil(db);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("mongodbUtil初始化失败");
			}
		}

		return mongodbUtil;
	}

}
