package com.konka.dhtsearch.db.mongodb.orm1;
import java.util.List;

import com.konka.dhtsearch.db.models.DhtInfo;
import com.mongodb.DB;
import com.mongodb.Mongo;


public class TestLoad {
	 
	public static void main(String args[]) throws Exception
	{
		Mongo m = new Mongo();
		DB db = m.getDB("test");
		MongodbUtil orm = new MongodbUtil(db);
		
//		List<Employee> employee = orm.loadAll(Employee.class);
		List<DhtInfo> project = orm.findAll(DhtInfo.class);
//		List<Manager> manager = orm.loadAll(Manager.class);
		System.out.println(project.size());
	}
}
