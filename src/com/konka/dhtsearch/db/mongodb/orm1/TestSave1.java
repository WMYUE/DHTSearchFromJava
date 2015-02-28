package com.konka.dhtsearch.db.mongodb.orm1;

import java.util.ArrayList;
import java.util.List;

import com.konka.dhtsearch.db.models.DhtInfo;
import com.konka.dhtsearch.parser.MultiFile;
import com.mongodb.DB;
import com.mongodb.Mongo;

public class TestSave1 {
	public static void main(String args[]) throws Exception {
		Mongo m = new Mongo("localhost", 27017);
		DB db = m.getDB("test");

//		db.dropDatabase();
		MongodbUtil orm = new MongodbUtil(db);

		List<MultiFile> lists = new ArrayList<>();

		for (int i = 0; i < 5; i++) {
			MultiFile multiFile = new MultiFile();
			multiFile.setPath("c:/ddd/we/ddd" + i);
			multiFile.setSingleFileLength(123545222l);
			lists.add(multiFile);
		}
		DhtInfo dhtInfo = new DhtInfo();
		dhtInfo.setAnalysised(200);
		dhtInfo.setCreateTime(123456755);
		dhtInfo.setFileList(lists);
		dhtInfo.setFileName("我是文件名");
		dhtInfo.setFileSize(12345675);
		dhtInfo.setInfo_hash("125515241534531fds");
		dhtInfo.setLastRequestsTime(125454125121L);
		dhtInfo.setPeerIp("127.0.0.1:8811");
		dhtInfo.setTag("tag");
		dhtInfo.setTorrentFilePath("path");
		orm.save(dhtInfo);
		System.out.println("dddd");
	}
}
