package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaircc.torrent.bencoding.BDecodingException;
import org.yaircc.torrent.bencoding.BTypeException;

import com.konka.dhtsearch.db.DaoFactory;
import com.konka.dhtsearch.db.DhtInfoStateCode;
import com.konka.dhtsearch.db.dao.DhtInfoDao;
import com.konka.dhtsearch.db.exception.DhtException;
import com.konka.dhtsearch.db.models.DhtInfo;
import com.konka.dhtsearch.parser.TorrentInfo;
import com.konka.dhtsearch.util.HttpUrlUtils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * 发送消息查找node
 * 
 * @author 耳东 (cgp@0731life.com)
 *
 */
public class KadParserTorrentServer implements Runnable {

	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;
	private final OkHttpClient client = new OkHttpClient();

	public KadParserTorrentServer() {
		startThread = new Thread(this);
		client.getDispatcher().setMaxRequests(5);
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
			try {

				HttpUrlUtils httpUrlUtils = new HttpUrlUtils();
				DhtInfoDao dhtInfoDao = DaoFactory.getDhtInfoDao();
				List<DhtInfo> dhtInfos = dhtInfoDao.getNoAnalyticDhtInfos(50);
				for (DhtInfo dhtInfo : dhtInfos) {
					String info_hash = dhtInfo.getInfo_hash().trim().toUpperCase();
					String baseurl = "http://bt.box.n0808.com/%1$s/%2$s/%3$s.torrent";
					String url = String.format(baseurl, info_hash.substring(0, 2), info_hash.substring(info_hash.length() - 2, info_hash.length()), info_hash);
					Request request1 = new Request.Builder().url(url).build();
//					client.newCall(request1).enqueue(new MyCallback(dhtInfo));
//					Thread.sleep(3000);

					try {
						com.konka.dhtsearch.util.Request request = new com.konka.dhtsearch.util.Request(url);
						InputStream inputStream = httpUrlUtils.performRequest(request);
						try {
							TorrentInfo torrentInfo = new TorrentInfo(inputStream);
							dhtInfo.setCreateTime(torrentInfo.getCreattime());

							dhtInfo.setFileName(torrentInfo.getName());
							dhtInfo.setFileSize(torrentInfo.getFilelenth());

							if (!torrentInfo.isSingerFile()) {
								dhtInfo.setFileList(torrentInfo.getMultiFiles().get(0).getPath());
							}
							// mDhtInfo.setTorrentFilePath(torrentFilePath);//种子保存地址
							dhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOADSUCCESS_AND_PARSING_SUCCESS);
							DaoFactory.getDhtInfoDao().update(dhtInfo);
						} catch (Exception e) {
							try {
								dhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_SUCCESS_BUT_PARSING_FAILED);
								DaoFactory.getDhtInfoDao().update(dhtInfo);
							} catch (DhtException e1) {
								e1.printStackTrace();
							}
						}
					} catch (Exception e2) {// 这里发生下载错误
						dhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_FAILED);
						DaoFactory.getDhtInfoDao().update(dhtInfo);

					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

//	class MyCallback implements Callback {
//		private DhtInfo mDhtInfo;
//
//		public MyCallback(DhtInfo dhtInfo) {
//			super();
//			this.mDhtInfo = dhtInfo;
//		}
//
//		@Override
//		public void onFailure(Request request, IOException e) {
//			mDhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_FAILED);
//			try {
//				DaoFactory.getDhtInfoDao().update(mDhtInfo);
//			} catch (DhtException e1) {
//				e1.printStackTrace();
//			}
//		}
//
//		@Override
//		public void onResponse(Response response) throws IOException {
//			System.out.println("响应码=" + response.code());
//			if (200 != response.code()) {
//				mDhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_FAILED);
//				try {
//					DaoFactory.getDhtInfoDao().update(mDhtInfo);
//				} catch (DhtException e1) {
//					e1.printStackTrace();
//				}
//				return;
//			}
//			InputStream inputStream = response.body().byteStream();
//			try {
//				TorrentInfo torrentInfo = new TorrentInfo(inputStream);
//				mDhtInfo.setCreateTime(torrentInfo.getCreattime());
//
//				mDhtInfo.setFileName(torrentInfo.getName());
//				mDhtInfo.setFileSize(torrentInfo.getFilelenth());
//
//				if (!torrentInfo.isSingerFile()) {
//					mDhtInfo.setFileList(torrentInfo.getMultiFiles().get(0).getPath());
//				}
//
//				// mDhtInfo.setTorrentFilePath(torrentFilePath);//种子保存地址
//
//				mDhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOADSUCCESS_AND_PARSING_SUCCESS);
//				DaoFactory.getDhtInfoDao().update(mDhtInfo);
//
//			} catch (BDecodingException | BTypeException | DhtException e) {
//				e.printStackTrace();
//				try {
//					mDhtInfo.setAnalysised(DhtInfoStateCode.DOWNLOAD_SUCCESS_BUT_PARSING_FAILED);
//					DaoFactory.getDhtInfoDao().update(mDhtInfo);
//				} catch (DhtException e1) {
//					e1.printStackTrace();
//				}
//			}
//		}
//	}

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
