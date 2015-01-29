package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeResponse;

/**
 * 守护线程，负责接受和发送消息
 * 
 */
public class KadServer implements Runnable {

	private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> pkts = new LinkedBlockingDeque<DatagramPacket>();;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(10);
	private final AtomicBoolean isActive = new AtomicBoolean(false);

	public KadServer(DatagramSocket socket) {
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
		// System.out.println("KadServer: send: " + msg + " to: " +
		try {
			byte[] buf = BEncodedOutputStream.bencode(msg.getbMap());
			final DatagramPacket pkt = new DatagramPacket(buf, 0, buf.length);

			pkt.setSocketAddress(to.getSocketAddress());
			this.socket.send(pkt);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 收到信息后处理
	private void handleIncomingPacket(final DatagramPacket pkt) {
		this.srvExecutor.execute(new Runnable() {// 交给线程池处理
					@Override
					public void run() {
						KadMessage msg = null;
						try {// 这里处理消息的方法需要重写
							BMap bMap = (BMap) BEncodedInputStream.bdecode(pkt.getData());
							if (bMap.containsKey("y")) {
								String y = bMap.getString("y");
								String transaction = bMap.getString("t");

								if ("q".equals(y)) {// 对方请求

									if (bMap.containsKey("q")) {
										String q = bMap.getString("q");// find_node or getpeers===

										switch (q) {
											case "find_node":

												break;
											case "get_peers":

												break;
											case "ping"://

												break;

											default:
												break;
										}

									} else {
										return;
									}
								} else if ("r".equals(y)) {// 对方的响应
									// TODO 响应的操作应该根据请求的id t判断是哪个响应，t清楚一次必须改变
									if (bMap.containsKey("r")) {
										BMap bMap_r = bMap.getMap("r");
										if (bMap_r.containsKey("token")) {// 只有getpeers响应中是toten
											// TODO 解析getpeers响应
										} else if (bMap_r.containsKey("nodes")) {// 除了 getpeers 就只有findnode有nodes了，所有这里是findnode响应
											List<Node> nodes = passNodes(bMap_r.getString("nodes"));
											Node src = new Node();
											src.setInetAddress(pkt.getAddress());// InetAddress

											FindNodeResponse msg1 = new FindNodeResponse(transaction, src);
											msg1.setNodes(nodes);
										}
									}

									MessageDispatcher messageDispatcher = AppManager.getMessageDispatcherManager().findMessageDispatcherByTag(transaction);
									if (messageDispatcher != null) {
										messageDispatcher.handle(msg);
									}
								}

							}

						} catch (final Exception e) {
							e.printStackTrace();
							return;
						} finally {
							KadServer.this.pkts.offer(pkt);// 如果可以，将ptk加入到队列
						}

						// appManager.getMessageDispatcherManager().findMessageDispatcherByTag(tag);
						// y=q q=r 请求--- y=r r= ---响应，可以判断是否是别人请求还是响应

						// t="aa"，可以根据t的值判断是哪个请求的返回信息

						// MessageDispatcher 中的att可以作为一个标识

						// msg.getSrc().g
						// 问题，这里我怎么知道msg是什么消息呢？？？
						// call all the expecters
						// final List<MessageDispatcher<?>> shouldHandle = extractShouldHandle(msg);
						//
						// for (final MessageDispatcher<?> m : shouldHandle)
						// try {
						// m.handle(msg);
						// } catch (final Exception e) {
						// // handle fail should not interrupt other handlers
						// e.printStackTrace();
						// }
					}

				});
	}

	/**
	 * 解析出nodes
	 * 
	 * @param string
	 *            nodes集合的字符串
	 */
	private List<Node> passNodes(String string) {
		return null;
	}

	/**
	 * The server loop:
	 * 
	 * @category accept a message from socket
	 * @category parse message
	 * @category handle the message in a thread pool 这个线程用来接受信息
	 */
	@Override
	public void run() {
		this.isActive.set(true);

		while (this.isActive.get()) {
			DatagramPacket pkt = null;
			try {
				System.out.println("等待数据");
				pkt = this.pkts.poll();

				if (pkt == null)
					pkt = new DatagramPacket(new byte[1024 * 64], 1024 * 64);

				this.socket.receive(pkt);// 堵塞
				System.out.println("已经拿到数据可");
				handleIncomingPacket(pkt);// 收到信息后处理

			} catch (final Exception e) {
				// insert the taken pkt back
				if (pkt != null)
					this.pkts.offer(pkt);

				e.printStackTrace();
			}

		}
	}

	/**
	 * Shutdown the server and closes the socket 关闭服务
	 * 
	 * @param kadServerThread
	 */
	// @Override
	public void shutdown(final Thread kadServerThread) {
		this.isActive.set(false);
		this.socket.close();
		kadServerThread.interrupt();
		try {
			kadServerThread.join();
		} catch (final InterruptedException e) {
		}
	}

}
