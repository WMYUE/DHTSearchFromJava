package com.konka.dhtsearch.db.mysql.dao;

import java.util.List;
import java.util.Map;

import com.konka.dhtsearch.db.models.DhtInfo;
import com.konka.dhtsearch.db.mysql.exception.DhtException;

public interface DhtInfoDao {

	public void insert(DhtInfo dhtInfo) throws DhtException;

	public void delete(DhtInfo dhtInfo) throws DhtException;

	public void update(DhtInfo dhtInfo) throws DhtException;

	public DhtInfo findById(Integer iddhtInfo) throws DhtException;

	public List<DhtInfo> findAll() throws DhtException;

	public List<DhtInfo> getNoAnalyticDhtInfos(int count) throws DhtException;

}
