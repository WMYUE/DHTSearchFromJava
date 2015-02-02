package com.konka.dhtsearch.bittorrentkad;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.KeybasedRouting;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.bucket.KadBuckets;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.net.KadServer;

public class KadNet implements KeybasedRouting {
	// private final KadBuckets findValueOperation;// 查找相识节点用

	private final KadServer kadServer;// = AppManager.getKadServer();// Runnable 主要是TODO KadServer
	private final KadBuckets kadBuckets = AppManager.getKadBuckets();// 路由表
	private final int bucketSize = 8;// 一个k桶大小
	private final BootstrapNodesSaver bootstrapNodesSaver;// 关机后保存到本地，启动时候从本地文件中加载
	private final BlockingQueue<DatagramPacket> nodesqueue = new LinkedBlockingDeque<DatagramPacket>();

	private Thread kadServerThread = null;

	/**
	 * @param findValueOperation
	 *            查找相识节点用
	 * @param kadServer
	 *            服务
	 * @param kadBuckets
	 *            路由表
	 * @param bootstrapNodesSaver
	 *            保存数据用
	 * @throws SocketException 
	 */
	public KadNet(BootstrapNodesSaver bootstrapNodesSaver) throws SocketException {
		this.bootstrapNodesSaver = bootstrapNodesSaver;
		DatagramSocket socket = null;
		socket = new DatagramSocket(AppManager.getLocalNode().getSocketAddress());
		this.kadServer = new KadServer(socket);
	}

	@Override
	public void create() throws IOException {

		kadBuckets.registerIncomingMessageHandler();
		kadServerThread = new Thread(kadServer);
		kadServerThread.start();
		if (bootstrapNodesSaver != null) {
			bootstrapNodesSaver.load();
			bootstrapNodesSaver.start();
		}
	}

	/**
	 * 加入已知节点uri
	 */
	@Override
	public void join(Collection<URI> bootstraps) {
		// joinOperation.addBootstrap(bootstraps).doJoin();
	}

	@Override
	public List<Node> findNode(Key k) {// 根据k返回相似节点
		List<Node> result = kadBuckets.getClosestNodesByKey(k, 8);

		List<Node> $ = new ArrayList<Node>(result);

		if ($.size() > bucketSize)
			$.subList(bucketSize, $.size()).clear();

		return result;
	}

	@Override
	public List<Node> getNeighbours() {
		return kadBuckets.getAllNodes();
	}

	@Override
	public Node getLocalNode() {
		return AppManager.getLocalNode();
	}

	@Override
	public String toString() {
		return getLocalNode().toString() + "\n" + kadBuckets.toString();
	}

	@Override
	public void sendMessage(Node to, KadMessage msg) throws IOException {

		kadServer.send(to, msg);
	}

	@Override
	public void shutdown() {
		try {
			if (bootstrapNodesSaver != null) {
				bootstrapNodesSaver.saveNow();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		kadServer.shutdown(kadServerThread);
	}
}
