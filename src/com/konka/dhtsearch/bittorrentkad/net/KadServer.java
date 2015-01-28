package com.konka.dhtsearch.bittorrentkad.net;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;

/**
 * Low level communication handler. This class does all the serialze/de-serialze and socket programming.
 * 
 * @author eyal.kibbar@gmail.com
 * 
 */
public class KadServer   implements Runnable{

	// dependencies
//	private final KadSerializer serializer;
	private final DatagramSocket sockProvider;
	private final BlockingQueue<DatagramPacket> pkts;
	private final ExecutorService srvExecutor;
	private final Set<MessageDispatcher<?>> expecters;// 过滤器在这里
	private final Set<MessageDispatcher<?>> nonConsumableExpecters;
	private final String kadScheme;


	// state
	private final AtomicBoolean isActive = new AtomicBoolean(false);

	// private final BlockingQueue<DatagramPacket> pktsout;

	KadServer( final String kadScheme, final DatagramSocket sockProvider, //
			final BlockingQueue<DatagramPacket> pkts, final BlockingQueue<DatagramPacket> pktsout,//
			final ExecutorService srvExecutor, final Set<MessageDispatcher<?>> expecters, //
			final Set<MessageDispatcher<?>> nonConsumableExpecters //

	) {

		this.kadScheme = kadScheme;
//		this.serializer = serializer;
		this.sockProvider = sockProvider;
		this.pkts = pkts;
		// this.pktsout = pktsout;
		this.srvExecutor = srvExecutor;
		this.expecters = expecters;
		this.nonConsumableExpecters = nonConsumableExpecters;
	}

	/**
	 * 真正发送网络数据报消息
	 * 
	 * @param to
	 *            the destination node
	 * @param msg
	 *            the message to be sent
	 * @throws IOException
	 *             any socket exception
	 */
//	@Override
	public void send(final Node to, final KadMessage msg) throws IOException {
		// System.out.println("KadServer: send: " + msg + " to: " +
		// to.getKey());

//		if (msg instanceof PingRequest)
//			this.nrOutgoingPings.incrementAndGet();

		ByteArrayOutputStream bout = null;

		try {
			bout = new ByteArrayOutputStream();
//			this.serializer.write(msg, bout);
			// here is the memory allocated.
			final byte[] bytes = bout.toByteArray();
//			this.nrBytesSent.addAndGet(bytes.length);

			final DatagramPacket pkt = new DatagramPacket(bytes, 0, bytes.length);

			pkt.setSocketAddress(to.getSocketAddress(this.kadScheme));
			this.sockProvider.send(pkt);

		} finally {
			try {

				bout.close();
				bout = null;

			} catch (final Exception e) {
			}
		}
	}

	/**
	 * xxx
	 * 
	 * @param msg
	 * @return
	 */
	private List<MessageDispatcher<?>> extractShouldHandle(final KadMessage msg) {
		List<MessageDispatcher<?>> shouldHandle = Collections.emptyList();
		List<MessageDispatcher<?>> nonConsumableShouldHandle = Collections.emptyList();
		final List<MessageDispatcher<?>> $ = new ArrayList<MessageDispatcher<?>>();
		synchronized (this.expecters) {
			if (!this.expecters.isEmpty())
				shouldHandle = filter(having(on(MessageDispatcher.class)//
						.shouldHandleMessage(msg), is(true)), this.expecters);
		}

		synchronized (this.nonConsumableExpecters) {
			if (!this.nonConsumableExpecters.isEmpty())
				nonConsumableShouldHandle = filter(having(on(MessageDispatcher.class)//
						.shouldHandleMessage(msg), is(true)), this.nonConsumableExpecters);
		}

		$.addAll(nonConsumableShouldHandle);
		$.addAll(shouldHandle);
		return $;
	}

	// 收到信息后处理
	private void handleIncomingPacket(final DatagramPacket pkt) {
//		this.nrIncomingMessages.incrementAndGet();// i++
//		this.nrBytesRecved.addAndGet(pkt.getLength());// 收到的数据大小，自动累加
		this.srvExecutor.execute(new Runnable() {// 交给线程池处理

					@Override
					public void run() {
						ByteArrayInputStream bin = null;
						KadMessage msg = null;
						try {// 这里处理消息的方法需要重写
							bin = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
//							msg = KadServer.this.serializer.read(bin);// 这里应该是解码

							// System.out.println("KadServer: handleIncomingPacket: " +
							// msg + " from: " + msg.getSrc().getKey());

							// fix incoming src address
							msg.getSrc().setInetAddress(pkt.getAddress());// InetAddress
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
						//问题，这里我怎么知道msg是什么消息呢？？？
						// call all the expecters
						final List<MessageDispatcher<?>> shouldHandle = extractShouldHandle(msg);

						for (final MessageDispatcher<?> m : shouldHandle)
							try {
								m.handle(msg);
							} catch (final Exception e) {
								// handle fail should not interrupt other handlers
								e.printStackTrace();
							}
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

				this.sockProvider.receive(pkt);// 堵塞
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
//	@Override
	public void shutdown(final Thread kadServerThread) {
		this.isActive.set(false);
		this.sockProvider.close();
		kadServerThread.interrupt();
		try {
			kadServerThread.join();
		} catch (final InterruptedException e) {
		}
	}

}
