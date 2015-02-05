package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.bucket.Bucket;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;

public class KadSendMsgServer implements Runnable {

	private final DatagramSocket socket;
//	private final BlockingQueue<Node> nodes;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(10);
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;// ;=new Thread();
	private final  Bucket  kadBuckets;

	public KadSendMsgServer(DatagramSocket socket,  Bucket kadBuckets) {
		this.socket = socket;
		startThread = new Thread(this);
		this.kadBuckets = kadBuckets;
	}

	/**
	 * 只发送findnode操作，其他请求请使用KadSendMsgServer
	 * 
	 * @param msg
	 *            要发送的消息（一般是具体实现）
	 * @throws IOException
	 *             any socket exception
	 */
	public void send(final KadMessage msg) throws IOException {
		try {
			if(msg.getSrc().equals(AppManager.getLocalNode())){
				return;
			}
			byte[] buf = msg.getBencodeData();
			final DatagramPacket pkt = new DatagramPacket(buf, 0, buf.length);
			pkt.setSocketAddress(msg.getSrc().getSocketAddress());
			this.socket.send(pkt);
//			System.out.println("发送="+BEncodedInputStream.bdecode(buf));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 不停发送消息
	 */
	@Override
	public void run() {
		this.isActive.set(true);
		while (this.isActive.get()) {
			try {
//				final Node to = nodes.take();
				// srvExecutor.execute(new Runnable() {
				// @Override
				// public void run() {
				Thread.sleep(1000);
				List<KadNode> nodes=kadBuckets.getAllNodes();
				System.out.println("发数="+nodes.size());
				for(KadNode node:nodes){
					if(!node.getNode().equals(AppManager.getLocalNode())){
						send(node.getNode());
					}
				}
			
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private void send(Node to) {
		FindNodeRequest msg = FindNodeRequest.creatLocalFindNodeRequest(to);

		try {
			send(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shutdown the server and closes the socket 关闭服务
	 * 
	 * @param kadServerThread
	 */
	// @Override
	public void shutdown() {
		this.isActive.set(false);
		this.socket.close();
		startThread.interrupt();
		try {
			startThread.join();
		} catch (final InterruptedException e) {
		}
	}

	public void start() {
		startThread.start();
	}
}
