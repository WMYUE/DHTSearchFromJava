package com.konka.dhtsearch.db.mongodb.orm1;

import java.util.ArrayList;

import com.mongodb.DB;
import com.mongodb.Mongo;

public class TestSave {
	public static void main(String args[]) throws Exception {
		Mongo m = new Mongo("localhost", 27017);
		DB db = m.getDB("test");
		MongodbUtil orm = new MongodbUtil(db);
		// db.dropDatabase();

		Project project1 = new Project();
		Project project2 = new Project();
		project2.name = "project2";
		project1.name = "project1";
		ArrayList<Project> lists = new ArrayList<Project>();
		// ArrayList<Object> lists1=new ArrayList<Object>();
		// lists1.add("cgp cgp1");
		// lists1.add("cgp cgp1");
		// lists1.add("cgp cgp1");
		lists.add(project2);
		lists.add(project2);
		lists.add(project2);
		// project2.lists=lists1;
		lists.add(project2);
		project1.lists = lists;
		// project1.begin = new Date();
		orm.save(project1);

		// Project project2 = new Project();
		// project2.name = "project2";
		// project2.begin = new Date();
		// project2.end = new Date();
		//
		// Manager tombu = new Manager();
		// tombu.name = "tombu";
		// Employee parrt = new Employee();
		// parrt.name = "parrt";
		//
		// tombu.parkingSpot = 2;
		// tombu.yearlySalary = (float) 200;
		// tombu.directReports = new ArrayList<Employee>();
		// tombu.directReports.add(parrt);
		//
		// parrt.yearlySalary = (float) 100.5;
		// parrt.projects = new ArrayList<Project>();
		// parrt.projects.add(project1);
		// parrt.projects.add(project2);
		// parrt.manager = tombu;

		// orm.save(tombu);
		// orm.save(parrt);
		// orm.save(project2);
	}
}
