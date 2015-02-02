package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;

public class KadSendMsgServer implements Runnable {

	private final DatagramSocket socket;
	private final BlockingQueue<Node> nodes;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(10);
	private final AtomicBoolean isActive = new AtomicBoolean(false);

	public KadSendMsgServer(DatagramSocket socket, BlockingQueue<Node> nodes) {
		this.nodes = nodes;
		this.socket = socket;
	}

	/**
	 * 真正发送网络数据报消息
	 * 
	 * @param to
	 *            目的地的节点
	 * @param msg
	 *            要发送的消息（一般是具体实现）
	 * @throws IOException
	 *             any socket exception
	 */
	// @Override
	public void send(final Node to, final KadMessage msg) throws IOException {
		try {
			byte[] buf = msg.getBencodeData(to);
			final DatagramPacket pkt = new DatagramPacket(buf, 0, buf.length);
			pkt.setSocketAddress(to.getSocketAddress());
			this.socket.send(pkt);
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
				final Node to = nodes.take();
				srvExecutor.execute(new Runnable() {
					@Override
					public void run() {
						send(to);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private void send(Node to) {
		FindNodeRequest msg = FindNodeRequest.creatLocalFindNodeRequest(to);
		try {
			send(to, msg);
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
	public void shutdown(final Thread kadSendMsgServer) {
		this.isActive.set(false);
		this.socket.close();
		kadSendMsgServer.interrupt();
		try {
			kadSendMsgServer.join();
		} catch (final InterruptedException e) {
		}
	}

}
