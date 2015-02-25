package com.konka.dhtsearch.db.dao;

import java.util.List;

import com.konka.dhtsearch.db.exception.DhtException;
import com.konka.dhtsearch.db.models.DhtInfo;

public interface DhtInfoDao {

    public void insert(DhtInfo dhtInfo) throws DhtException;
    
    public void delete(DhtInfo dhtInfo) throws DhtException;
    
    public void update(DhtInfo dhtInfo) throws DhtException;
    
    public DhtInfo findById(Integer iddhtInfo) throws DhtException;
    
    public List<DhtInfo> findAll() throws DhtException;
    
}
