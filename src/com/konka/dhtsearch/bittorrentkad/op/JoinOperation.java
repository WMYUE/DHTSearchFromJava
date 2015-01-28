package com.konka.dhtsearch.bittorrentkad.op;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.bucket.KBuckets;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;
import com.konka.dhtsearch.bittorrentkad.msg.PingRequest;
import com.konka.dhtsearch.bittorrentkad.msg.PingResponse;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.IdMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;

/**
 * Join operation as defined in the kademlia algorithm
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class JoinOperation {

	// dependencies
	private final FindNodeOperation findNodeOperationProvider;
	private final PingRequest pingRequestProvider;
	private final MessageDispatcher<Void> msgDispatcherProvider;
	private final Key zeroKey;
	private final String kadScheme;
	private final KBuckets kBuckets;
	private final Node localNode;
	private final KadNode kadNodeProvider;
	// state
	private Collection<Node> bootstrap = new HashSet<Node>();

	JoinOperation(FindNodeOperation findNodeOperationProvider, PingRequest pingRequestProvider,//
			MessageDispatcher<Void> msgDispatcherProvider, KadNode kadNodeProvider, KBuckets kBuckets, //
			Key zeroKey, String kadScheme, Node localNode, Timer timer, long refreshInterval, //
			TimerTask refreshTask) {

		this.kadNodeProvider = kadNodeProvider;
		this.findNodeOperationProvider = findNodeOperationProvider;
		this.pingRequestProvider = pingRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.kBuckets = kBuckets;
		this.zeroKey = zeroKey;
		this.kadScheme = kadScheme;
		this.localNode = localNode;
	}   

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.op.JoinOperation#addBootstrap(java.util.Collection)
	 */
	public JoinOperation addBootstrap(Collection<URI> bootstrapUri) {

		for (URI uri : bootstrapUri) {

			Node n = new Node(zeroKey);
			try {
				System.out.println(InetAddress.getByName(uri.getHost()));
				n.setInetAddress(InetAddress.getByName(uri.getHost()));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				continue;
			}
			n.addEndpoint(kadScheme, uri.getPort());
			bootstrap.add(n);
		}

		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.op.JoinOperation#doJoin()
	 */
	public void doJoin() {

		final CountDownLatch latch = new CountDownLatch(bootstrap.size());
		CompletionHandler<KadMessage, Void> callback = new CompletionHandler<KadMessage, Void>() {

			@Override
			public void completed(KadMessage msg, Void nothing) {
				try {
					kBuckets.insert(kadNodeProvider.setNode(msg.getSrc()).setNodeWasContacted());
				} finally {
					latch.countDown();
				}
			}

			@Override
			public void failed(Throwable exc, Void nothing) {
				latch.countDown();
			}
		};

		for (Node n : bootstrap) {
			PingRequest pingRequest = pingRequestProvider;
			msgDispatcherProvider.addFilter(new IdMessageFilter(pingRequest.getId())).addFilter(new TypeMessageFilter(PingResponse.class)).setConsumable(true).setCallback(null, callback).send(n, pingRequest);
		}

		// waiting for responses

		try {
			latch.await();
		} catch (InterruptedException e1) {
			throw new RuntimeException(e1);
		}

		findNodeOperationProvider.setKey(localNode.getKey()).doFindNode();

		for (Key key : kBuckets.randomKeysForAllBuckets()) {
			findNodeOperationProvider.setKey(key).doFindNode();
		}

		if (kBuckets.getClosestNodesByKey(zeroKey, 1).isEmpty())
			throw new IllegalStateException("all bootstrap nodes are down");

		try {
			// timer.scheduleAtFixedRate(refreshTask, refreshInterval, refreshInterval);
		} catch (IllegalStateException e) {
			// if I couldn't schedule the refresh task i don't care
		}
	}

}
