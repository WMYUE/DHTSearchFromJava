package org.konkakjb.text;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.db.mysql.exception.DhtException;
import com.konka.dhtsearch.exception.ErrHandler;
import com.konka.dhtsearch.util.ThreadUtil;

public class SearchText {
	private static final InetSocketAddress[] BOOTSTRAP_NODES = { //
	new InetSocketAddress("router.bittorrent.com", 6881), //
			new InetSocketAddress("dht.transmissionbt.com", 6881),//
			new InetSocketAddress("router.utorrent.com", 6881), };

	public static void main(String[] args) throws DhtException {
			startservice();
	}

	public static void startservice() {
		int size = 3;
		try {
			for (int i = 0; i < size; i++) {
				AppManager.init();// 1---
				Key key = AppManager.getKeyFactory().generate();
				Node localNode = new Node(key).setInetAddress(InetAddress.getByName("0.0.0.0")).setPoint(20200 + i);// 这里注意InetAddress.getLocalHost();为空
				// new KadNet(null, localNode).create();
//				 new KadNet(null, localNode).join(BOOTSTRAP_NODES).create();
				startKadNet(localNode);
			}
			// new KadParserTorrentServer().start();// 启动种子下载服务
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void startKadNet(final Node localNode) {
		try {
			KadNet target = new KadNet(null, localNode).join(BOOTSTRAP_NODES);
			Thread thread = new Thread(target);
			thread.setUncaughtExceptionHandler(new ErrHandler() {
				@Override
				public void caughtEnd() {
					startKadNet(localNode);
				}
			});
			thread.setDaemon(true);
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private List<String> list = new LinkedList<String>();

	public static void text() throws NoSuchFieldException, SecurityException {
		ParameterizedType pt = (ParameterizedType) SearchText.class.getDeclaredField("list").getGenericType();
		System.out.println(pt.getActualTypeArguments().length);
		System.out.println(pt.getActualTypeArguments()[0]);
	}
}
