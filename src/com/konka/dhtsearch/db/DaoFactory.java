package com.konka.dhtsearch.db;

import com.konka.dhtsearch.db.dao.DhtInfoDao;
import com.konka.dhtsearch.db.dao.DhtInfoDaoJdbcImpl;


public class DaoFactory {

	public static DhtInfoDao getDhtInfoDao(){
		return DhtInfoDaoJdbcImpl.getInstance();
	}

}
