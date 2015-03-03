package com.konka.dhtsearch.db.luncene;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.konka.dhtsearch.db.models.DhtInfo_MongoDbPojo;
import com.konka.dhtsearch.db.mongodb.orm.MongodbUtil;
import com.konka.dhtsearch.parser.TorrentInfo;
import com.konka.dhtsearch.util.FilterUtil;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;

public class LuceneUtils {
	/**
	 * lucene保存的路径
	 */
	public static final String LUCENE_FILEPATH = "D://lucene.index";
	// 分词的字段
	public static final String KEYWORD = "Keyword";
	// -------------不分词的字段
	public static final String INFO_HASH_FIELD = "info_hash";
	// 数据字段
	public static final String TORRENTINFO_FIELD = "torrentInfo";

	public static final int PAGE_COUNT = 201;// 每页显示的数

	public static final int HITSPERPAGE_COUNT = 20000;// 查询结构总数

	// DhtInfo_MongoDbPojo
	public static void createIndex() throws Exception {

		MongodbUtil mongodbUtil = getMongodbUtil();
		DBCursor cursor = mongodbUtil.findDBCursor(DhtInfo_MongoDbPojo.class);

		Directory index = FSDirectory.open(new File(LUCENE_FILEPATH));

		Analyzer analyzer = new IKAnalyzer();// 这里要换成ik
		// StandardAnalyzer analyzer = new StandardAnalyzer();// 这里要换成ik
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
		IndexWriter indexWriter = new IndexWriter(index, config);
		indexWriter.deleteAll();
		System.out.println(cursor.count());
		while (cursor.hasNext()) {
			Document doc = new Document();

			DBObject object = cursor.next();
			System.out.println(object);
			DhtInfo_MongoDbPojo dhtInfo_MongoDbPojo = mongodbUtil.loadOne(DhtInfo_MongoDbPojo.class, object);
			TorrentInfo torrentInfo = dhtInfo_MongoDbPojo.getTorrentInfo();
			if (torrentInfo == null || FilterUtil.checkVideoType(torrentInfo)) {// 检测文件类型
				continue;
			}

			doc.add(new StringField(INFO_HASH_FIELD, dhtInfo_MongoDbPojo.getInfo_hash(), Field.Store.YES));// StringField不参加分词
			doc.add(new StringField(TORRENTINFO_FIELD, object.get(TORRENTINFO_FIELD).toString(), Field.Store.YES));// StringField不参加分词
			doc.add(new TextField(KEYWORD, torrentInfo.getNeedSegmentationString(), Field.Store.YES));// 多文件的文件名

			indexWriter.addDocument(doc);
			System.out.println("ok");
		}
		cursor.close();
		indexWriter.close();
	}

	public static MongodbUtil getMongodbUtil() throws UnknownHostException {
		Mongo mongoClient = new Mongo();
		DB db = mongoClient.getDB("test");
		MongodbUtil mongodbUtil = new MongodbUtil(db);
		return mongodbUtil;
	}

	/**
	 * 关键字搜索
	 * 
	 * @param searchString
	 *            搜索的字符串
	 * @param page
	 *            页码
	 * @throws Exception
	 */
	public static List<DhtInfo_MongoDbPojo> search(String searchString, int page) throws Exception {
		Directory index = FSDirectory.open(new File(LUCENE_FILEPATH));

		BooleanClause.Occur[] clauses = { BooleanClause.Occur.SHOULD };
		String[] searchFields = { KEYWORD };
		String[] searchFields11 = { searchString };
		// new QueryParser
		// QueryParser qp = new QueryParser(Version.LUCENE_40, fieldName, analyzer);
		// Query qp = new QueryParser(Version.LUCENE_34, fieldName, analyzer);
		Query query = MultiFieldQueryParser.parse(Version.LUCENE_4_9,searchFields11, searchFields, clauses, new IKAnalyzer());
		// IKAnalyzer.
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		// TopScoreDocCollector.
		TopScoreDocCollector collector = TopScoreDocCollector.create(HITSPERPAGE_COUNT,true);
		searcher.search(query, collector);

		ScoreDoc[] hits = collector.topDocs((page - 1) * PAGE_COUNT, PAGE_COUNT).scoreDocs; // 进行分页过滤
		// 4. display results
		System.out.println("Found " + hits.length + " hits.");

		List<DhtInfo_MongoDbPojo> dhtInfo_MongoDbPojos = new ArrayList<>();
		for (int i = 0; i < hits.length; ++i) {
			Document document = searcher.doc(hits[i].doc);
			DBObject object = (DBObject) JSON.parse(document.get(TORRENTINFO_FIELD));
			 System.out.println(object);
			if (object == null)
				continue;

			TorrentInfo torrentInfo = getMongodbUtil().loadOne(TorrentInfo.class, object);
			DhtInfo_MongoDbPojo dhtInfo_MongoDbPojo = new DhtInfo_MongoDbPojo();
			dhtInfo_MongoDbPojo.setInfo_hash(document.get(INFO_HASH_FIELD));
			dhtInfo_MongoDbPojo.setTorrentInfo(torrentInfo);

			dhtInfo_MongoDbPojos.add(dhtInfo_MongoDbPojo);
			// System.out.println(object);

		}
		reader.close();

		return dhtInfo_MongoDbPojos;
	}

	public static void main(String[] args) throws Exception {
		args = new String[] { "index", "com_konka_dhtsearch_db_models_DhtInfo_MongoDbPojo", "fileName" };
		 args = new String[] { "Femme" };
		if (args[0].equals("index")) {
			createIndex();
		} else {
			search(args[0], 1);
		}
		// String str = "中华人民币汇改";
		// List<String> lists = Util.getWords(str, new StandardAnalyzer(luceneVersion));
		// for (String s : lists) {
		// System.out.println(s);
		// }
	}
}
