package com.konka.dhtsearch.bittorrentkad.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;

/**
 * 守护线程，负责接受和发送消息
 * 
 */
public class KadServer implements Runnable {

	private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> pkts = new LinkedBlockingDeque<DatagramPacket>();;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(10);
	private final AtomicBoolean isActive = new AtomicBoolean(false);
//	private AppManager appManager = new AppManager();

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
		// to.getKey());

		// if (msg instanceof PingRequest)
		// this.nrOutgoingPings.incrementAndGet();

		ByteArrayOutputStream bout = null;

		try {
			bout = new ByteArrayOutputStream();
			// this.serializer.write(msg, bout);
			// here is the memory allocated.
			final byte[] bytes = bout.toByteArray();
			// this.nrBytesSent.addAndGet(bytes.length);

			final DatagramPacket pkt = new DatagramPacket(bytes, 0, bytes.length);

			pkt.setSocketAddress(to.getSocketAddress());
			this.socket.send(pkt);

		} finally {
			try {

				bout.close();
				bout = null;

			} catch (final Exception e) {
			}
		}
	}

	// 收到信息后处理
	private void handleIncomingPacket(final DatagramPacket pkt) {
		// this.nrIncomingMessages.incrementAndGet();// i++
		// this.nrBytesRecved.addAndGet(pkt.getLength());// 收到的数据大小，自动累加
		this.srvExecutor.execute(new Runnable() {// 交给线程池处理

					@Override
					public void run() {
						ByteArrayInputStream bin = null;
						KadMessage msg = null;
						try {// 这里处理消息的方法需要重写
							bin = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
							// msg = KadServer.this.serializer.read(bin);// 这里应该是解码

							// System.out.println("KadServer: handleIncomingPacket: " +
							// msg + " from: " + msg.getSrc().getKey());

							// fix incoming src address

							BMap bMap = (BMap) BEncodedInputStream.bdecode(pkt.getData());
							if (bMap.containsKey("y")) {
								String y = bMap.getString("y");
								String tag = bMap.getString("t");
								msg.getSrc().setInetAddress(pkt.getAddress());// InetAddress
								if ("q".equals(y)) {// 对方请求

								} else if ("r".equals(y)) {// 对方的响应
									// bMap.containsKey(key);

									MessageDispatcher messageDispatcher = AppManager.getMessageDispatcherManager().findMessageDispatcherByTag(tag);
									if (messageDispatcher != null) {
										messageDispatcher.handle(msg);
									}
								}

							}
							
						} catch (final Exception e) {
							e.printStackTrace();
							return;
						} finally {
							try {
								bin.close();
							} catch (final Exception e) {
							}
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
				pkt = this.pkts.poll();
				if (pkt == null)
					pkt = new DatagramPacket(new byte[1024 * 64], 1024 * 64);

				this.socket.receive(pkt);// 堵塞
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
