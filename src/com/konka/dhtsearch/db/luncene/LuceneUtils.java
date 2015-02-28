package com.konka.dhtsearch.db.luncene;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class LuceneUtils {
	public static final Version luceneVersion = Version.LUCENE_46;

	// DhtInfo_MongoDbPojo
	public static void createIndex(String tableName, String fieldName) throws Exception {
		Mongo mongoClient = new Mongo();
		DB db = mongoClient.getDB("test");
		DBCollection table = db.getCollection(tableName);
		DBCursor cursor = table.find();

		Directory index = FSDirectory.open(new File("lucene.index"));

		StandardAnalyzer analyzer = new StandardAnalyzer(luceneVersion);
		IndexWriterConfig config = new IndexWriterConfig(luceneVersion, analyzer);
		IndexWriter indexWriter = new IndexWriter(index, config);
		System.out.println(cursor.count());
		while (cursor.hasNext()) {
			DBObject object = cursor.next();
			Document doc = new Document();
			doc.add(new StringField("info_hash", object.get("info_hash").toString(), Field.Store.YES));// StringField不参加分词
			doc.add(new TextField("fieldName", object.get(fieldName).toString(), Field.Store.YES));
			indexWriter.addDocument(doc);
		}
		cursor.close();
		indexWriter.close();
		System.out.println("ok");
	}

//	static ScoreDoc after = null;

	public static void main(String[] args) throws Exception {
		 args = new String[] { "index", "com_konka_dhtsearch_db_models_DhtInfo", "fileName" };
		 args = new String[]{"我是"};
		 if (args[0].equals("index")) {
			 createIndex(args[1], args[2]);
		 } else {
		 search(args[0]);
		 }
//		String str = "中华人民币汇改";
//		List<String> lists = Util.getWords(str, new StandardAnalyzer(luceneVersion));
//		for (String s : lists) {
//			System.out.println(s);
//		}
	}
	static void search(String searchString) throws Exception {
		Directory index = FSDirectory.open(new File("lucene.index"));
		Query query = new QueryParser(luceneVersion, "text", new StandardAnalyzer(luceneVersion)).parse(searchString);
		// 3. search
		int hitsPerPage = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(query, collector);
		 
		//分页，  ScoreDoc[] hits = c.topDocs(first, max).scoreDocs;
		int first = 9,pageCount=2;
		ScoreDoc[] hits = collector.topDocs(first, pageCount).scoreDocs;  //进行分页过滤
		// 4. display results
		System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			Document d = searcher.doc(hits[i].doc);
			System.out.println((i + 1) + ". " + d.get("info_hash") + "\t" + d.get("text"));
			// d.g
		}

		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close();
	}
}
