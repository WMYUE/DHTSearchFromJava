package com.konka.dhtsearch.db.mongodb;

import java.net.UnknownHostException;
import com.konka.dhtsearch.db.models.DhtInfo_MongoDbPojo;
import com.konka.dhtsearch.db.models.PeerInfo;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class MongodbUtilProvider {
	public static MongodbUtil mongodbUtil;

	public static MongodbUtil getMongodbUtil() {

		if (mongodbUtil == null) {
			try {
//				Mongo m = new Mongo("localhost", 27017);
				Mongo m = new Mongo("198.98.102.169", 27017);
				DB db = m.getDB("test");
//				db.dropDatabase();
				mongodbUtil = new MongodbUtil(db);
				init(mongodbUtil);
				init2(mongodbUtil);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("mongodbUtil初始化失败:error=" + e);
			}
		}

		return mongodbUtil;
	}

	static void init(MongodbUtil mongodbUtil) {
		DBCollection dbCollection=mongodbUtil.getDBCollection(DhtInfo_MongoDbPojo.class);
 
		BasicDBObject basicDBObject1=new BasicDBObject("info_hash", 1);
		
		BasicDBObject basicDBObject2=new BasicDBObject();
		basicDBObject2.put("unique", true);
		basicDBObject2.put("dropDups", true);
		dbCollection.ensureIndex(basicDBObject1, basicDBObject2);//创建唯一索引
 
//		db.Users.ensureIndex({name:1,sex:-1})
		
		
	}
	static void init2(MongodbUtil mongodbUtil) {
		DBCollection dbCollection=mongodbUtil.getDBCollection(PeerInfo.class);
		
		BasicDBObject basicDBObject1=new BasicDBObject();
		basicDBObject1.put("ipAddress", 1);
		basicDBObject1.put("port", 1);
		
		BasicDBObject basicDBObject2=new BasicDBObject();
		basicDBObject2.put("unique", true);
		basicDBObject2.put("dropDups", true);
		dbCollection.ensureIndex(basicDBObject1, basicDBObject2);//创建唯一索引
		
	}
}
