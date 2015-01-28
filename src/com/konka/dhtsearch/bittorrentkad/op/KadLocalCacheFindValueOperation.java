package com.konka.dhtsearch.bittorrentkad.op;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.konka.dhtsearch.KeyComparator;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.bucket.KBuckets;
import com.konka.dhtsearch.bittorrentkad.cache.KadCache;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.announce_peer.StoreMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.net.Communicator;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.IdMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;

/**
 * Kademlia find node operation with the caching algorithm suggested in the
 * article: send a store message to the last node who did not have the value
 * (list of nodes)
 * 
 * 
 */
public class KadLocalCacheFindValueOperation extends FindValueOperation implements CompletionHandler<KadMessage, Node> {

	// state
	private List<Node> knownClosestNodes;
	private final Set<Node> alreadyQueried;
	private final Set<Node> querying;
	private final List<Node> lastSentTo;
	private Node returnedCachedResults = null;
	private KeyComparator keyComparator;
	private final AtomicInteger nrMsgsSent;

	// dependencies
	private final FindNodeRequest findNodeRequestProvider;
	private final MessageDispatcher<Node> msgDispatcherProvider;
	private final KBuckets kBuckets;
	private final Node localNode;
	private final int kBucketSize;
	private final int nrShare;
	private final StoreMessage storeMessageProvider;
	private final Communicator kadServer;
	private final KadCache cache;

	private final AtomicInteger nrLocalCacheHits;
	private final AtomicInteger nrRemoteCacheHits;

	KadLocalCacheFindValueOperation( final Node localNode,
			final int kBucketSize,  final int nrShare,
			final FindNodeRequest findNodeRequestProvider, final MessageDispatcher<Node> msgDispatcherProvider,
			final KBuckets kBuckets, final StoreMessage storeMessageProvider, final Communicator kadServer,
			final KadCache cache,

			final AtomicInteger nrLocalCacheHits,
			 final AtomicInteger nrRemoteCacheHits) {

		this.localNode = localNode;
		this.kBucketSize = kBucketSize;
		this.kBuckets = kBuckets;
		this.nrShare = nrShare;
		this.findNodeRequestProvider = findNodeRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.storeMessageProvider = storeMessageProvider;
		this.kadServer = kadServer;
		this.cache = cache;
		this.nrMsgsSent = new AtomicInteger();

		this.alreadyQueried = new HashSet<Node>();
		this.querying = new HashSet<Node>();

		this.lastSentTo = new LinkedList<Node>();

		this.nrLocalCacheHits = nrLocalCacheHits;
		this.nrRemoteCacheHits = nrRemoteCacheHits;

	}

	@Override
	public int getNrQueried() {
		return this.nrMsgsSent.get();
	}

	private synchronized Node takeUnqueried() {
		for (int i = 0; i < this.knownClosestNodes.size(); ++i) {
			final Node n = this.knownClosestNodes.get(i);
			if (!this.querying.contains(n) && !this.alreadyQueried.contains(n)) {
				this.querying.add(n);
				return n;
			}
		}
		return null;
	}

	private boolean hasMoreToQuery() {
		return !this.querying.isEmpty() || !this.alreadyQueried.containsAll(this.knownClosestNodes);
	}

	private boolean trySendFindNode(final Node to) {
		final FindNodeRequest findNodeRequest = this.findNodeRequestProvider.setSearchCache(true).setKey(this.key);

		return this.msgDispatcherProvider.addFilter(new IdMessageFilter(findNodeRequest.getId()))
				.addFilter(new TypeMessageFilter(FindNodeResponse.class)).setConsumable(true).setCallback(to, this)
				.trySend(to, findNodeRequest);
	}

	private void sendFindNode(final Node to) {
		final FindNodeRequest findNodeRequest = this.findNodeRequestProvider.setSearchCache(true).setKey(this.key);

		this.msgDispatcherProvider.addFilter(new IdMessageFilter(findNodeRequest.getId()))
		.addFilter(new TypeMessageFilter(FindNodeResponse.class)).setConsumable(true).setCallback(to, this)
		.send(to, findNodeRequest);
	}

	private void sortKnownClosestNodes() {
		this.knownClosestNodes = sort(this.knownClosestNodes, on(Node.class).getKey(), this.keyComparator);
		if (this.knownClosestNodes.size() >= this.kBucketSize)
			this.knownClosestNodes.subList(this.kBucketSize, this.knownClosestNodes.size()).clear();
	}

	@Override
	public List<Node> doFindValue() {

		final List<Node> nodes = this.cache.search(this.key);
		if (nodes != null && nodes.size() >= this.kBucketSize) {
			this.nrLocalCacheHits.incrementAndGet();
			return nodes;
		}

		this.keyComparator = new KeyComparator(this.key);
		this.knownClosestNodes = this.kBuckets.getClosestNodesByKey(this.key, this.kBucketSize);
		this.knownClosestNodes.add(this.localNode);
		sortKnownClosestNodes();
		this.alreadyQueried.add(this.localNode);

		do {
			final Node node = takeUnqueried();

			synchronized (this) {
				// check if finished already...
				if (!hasMoreToQuery() || this.returnedCachedResults != null)
					break;

				if (node != null){
					if(!trySendFindNode(node)){
						try {
							if(this.querying.size() == 1)
							{
								//If only I try to send I send and block
								this.nrMsgsSent.incrementAndGet();
								sendFindNode(node);
							}
							else
							{
								//If there are pending msgs, i wait for their reply
								this.querying.remove(node);
								wait();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}else{
						this.nrMsgsSent.incrementAndGet();
					}
				}
				else
					if (!this.querying.isEmpty())
						try {
							wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
			}
		} while (true);

		this.knownClosestNodes = Collections.unmodifiableList(this.knownClosestNodes);

		if (this.returnedCachedResults != null)
			this.nrRemoteCacheHits.incrementAndGet();

		this.cache.insert(key, this.knownClosestNodes);
		return this.knownClosestNodes;
	}

	@Override
	public synchronized void completed(final KadMessage msg, final Node n) {
		notifyAll();
		this.querying.remove(n);
		this.alreadyQueried.add(n);

		if (this.returnedCachedResults != null)
			return;

		final List<Node> nodes = ((FindNodeResponse) msg).getNodes();
		nodes.removeAll(this.querying);
		nodes.removeAll(this.alreadyQueried);
		nodes.removeAll(this.knownClosestNodes);
		this.knownClosestNodes.addAll(nodes);
		sortKnownClosestNodes();

		if (((FindNodeResponse) msg).isCachedResults())
			this.returnedCachedResults = n;
		else {
			// listing n as last contacted nodes in the algorithm
			// that did not have the results in its cache
			this.lastSentTo.add(n);
			if (this.lastSentTo.size() > this.nrShare)
				this.lastSentTo.remove(0);
		}

	}

	@Override
	public synchronized void failed(final Throwable exc, final Node n) {
		notifyAll();
		this.querying.remove(n);
		this.alreadyQueried.add(n);
	}
}
