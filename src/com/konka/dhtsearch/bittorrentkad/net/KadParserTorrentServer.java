package com.konka.dhtsearch.bittorrentkad.net;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.konka.dhtsearch.db.DaoFactory;
import com.konka.dhtsearch.db.dao.DhtInfoDao;
import com.konka.dhtsearch.db.models.DhtInfo;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

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

		// Request request1 = new Request.Builder().url("http://www.konka.com").build();

		// client.newCall(request).enqueue(new Callback() {
		//
		// @Override
		// public void onFailure(Request request, IOException e) {
		// System.out.println(request);
		// }
		//
		// @Override
		// public void onResponse(Response response) throws IOException {
		// System.out.println(response.body().toString());
		// // response.cacheControl()
		// }
		// });
		// final Call c = client.newCall(request1);
		// try {
		// Thread.sleep(5000);
		// } catch (InterruptedException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		// c.enqueue(new Callback() {
		//
		// @Override
		// public void onFailure(Request request, IOException e) {
		// // request.
		// System.out.println(request.body().toString());
		// }
		//
		// @Override
		// public void onResponse(Response response) throws IOException {
		// System.out.println(new String(response.body().bytes()));
		// // response.cacheControl()
		// // c.cancel();
		// }
		// });

	}

	/**
	 * 不停发送消息
	 */
	@Override
	public void run() {
		this.isActive.set(true);
		while (this.isActive.get()) {
			try {
				Thread.sleep(1000);
				Request request = new Request.Builder().url("http://www.baidu.com").build();
				DhtInfoDao dhtInfoDao = DaoFactory.getDhtInfoDao();
				List<DhtInfo> dhtInfos = dhtInfoDao.getNoAnalyticDhtInfos(50);
				for (DhtInfo dhtInfo : dhtInfos) {
					String info_hash=dhtInfo.getInfo_hash().toUpperCase();
				}

			} catch (Exception e) {
				e.printStackTrace();
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
