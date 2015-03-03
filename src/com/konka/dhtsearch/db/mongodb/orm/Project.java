package com.konka.dhtsearch.db.mongodb.orm;

import java.util.List;

@MongoCollection
class Project {
//	@MongoField
	String name;
	
	public List<Project> lists;
//	@MongoField
//	Date begin;
//	@MongoField
//	Date end;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

 

	public List<Project> getLists() {
		return lists;
	}

	public void setLists(List<Project> lists) {
		this.lists = lists;
	}

	@Override
		public String toString() {
			 
			return "name="+name+"     lists="+lists.size()+"---"+lists.get(3).getClass();
		}
}