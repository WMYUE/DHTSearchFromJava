package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.konka.dhtsearch.db.models.DhtInfoStateCode;
import com.konka.dhtsearch.db.models.DhtInfo_MongoDbPojo;
import com.konka.dhtsearch.db.mongodb.MongodbUtil;
import com.konka.dhtsearch.db.mongodb.MongodbUtilProvider;
import com.konka.dhtsearch.db.mysql.exception.DhtException;
import com.konka.dhtsearch.parser.TorrentInfo;
import com.konka.dhtsearch.util.ArrayUtils;
import com.konka.dhtsearch.util.HttpUrlUtils;
import com.konka.dhtsearch.util.Request;
import com.konka.dhtsearch.util.ThreadUtil;

/**
 * 发送消息查找node
 * 
 * @author 耳东 (cgp@0731life.com)
 */
public class KadParserTorrentServer implements Runnable {

	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;
	private final HttpUrlUtils httpUrlUtils = new HttpUrlUtils();
	private final MongodbUtil dhtInfoDao = MongodbUtilProvider.getMongodbUtil();
	private final String baseurl = "http://bt.box.n0808.com/%1$s/%2$s/%3$s.torrent";

	public KadParserTorrentServer() {
		startThread = new Thread(this);
	}

	/**
	 * 不停发送消息
	 * 
	 * @category"http://bt.box.n0808.com/05/A5/05153F611B337A378F73F0D32D2C16D362D06BA5.torrent";
	 */
	@Override
	public void run() {
		this.isActive.set(true);
		while (this.isActive.get()) {

			List<DhtInfo_MongoDbPojo> dhtInfos = null;
			try {
				System.gc();
				dhtInfos = dhtInfoDao.getNoAnalyticDhtInfos(50);
			} catch (Exception e3) {
				e3.printStackTrace();
			}
			if (!ArrayUtils.isEmpty(dhtInfos)) {
				for (DhtInfo_MongoDbPojo dhtInfo : dhtInfos) {
					parseDhtInfo(dhtInfo);
				}
			}
			ThreadUtil.sleep(5 * 1000);

		}
	}

	private void parseDhtInfo(DhtInfo_MongoDbPojo dhtInfo) {
		System.out.println("parseDhtInfo");
		String info_hash = dhtInfo.getInfo_hash().trim().toUpperCase();
		String url = String.format(baseurl, info_hash.substring(0, 2), info_hash.substring(info_hash.length() - 2, info_hash.length()), info_hash);
		InputStream inputStream = null;
		try {
			Request request = new Request(url);
			inputStream = httpUrlUtils.performRequest(request);
			try {
				TorrentInfo torrentInfo = new TorrentInfo(inputStream);
				dhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOADSUCCESS_AND_PARSING_SUCCESS);
				dhtInfo.setTorrentInfo(torrentInfo);

				dhtInfoDao.update(dhtInfo);
				System.out.println("解析成功200");
				// DaoFactory.getDhtInfoDao().update(dhtInfo);
			} catch (Exception e) {// 解析出错
				try {
					dhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_SUCCESS_BUT_PARSING_FAILED);

					dhtInfoDao.update(dhtInfo);
					System.out.println("解析出错400");
					// DaoFactory.getDhtInfoDao().update(dhtInfo);
				} catch (DhtException e1) {
					e1.printStackTrace();
				}
			}
		} catch (Exception e2) {// 下载出错
			dhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_FAILED);
			try {
				dhtInfoDao.update(dhtInfo);
				System.out.println("下载出错1");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Shutdown the server and closes the socket 关闭服务
	 * 
	 * @param kadServerThread
	 */
	public void shutdown() {
		this.isActive.set(false);
		startThread.interrupt();
		try {
			startThread.join();
		} catch (final InterruptedException e) {
		}
	}

	public void start() {
		// startThread.setDaemon(true);
		startThread.start();
	}
}
