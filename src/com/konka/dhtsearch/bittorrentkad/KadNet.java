package com.konka.dhtsearch.bittorrentkad;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.KeyFactory;
import com.konka.dhtsearch.KeybasedRouting;
import com.konka.dhtsearch.MessageHandler;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.concurrent.FutureTransformer;
import com.konka.dhtsearch.bittorrentkad.handlers.FindNodeHandler;
import com.konka.dhtsearch.bittorrentkad.handlers.PingHandler;
import com.konka.dhtsearch.bittorrentkad.handlers.StoreHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.ContentMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersResponse;
import com.konka.dhtsearch.bittorrentkad.net.Communicator;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.IdMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TagMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;
import com.konka.dhtsearch.bittorrentkad.op.FindValueOperation;
import com.konka.dhtsearch.bittorrentkad.op.JoinOperation;

public class KadNet implements KeybasedRouting {

	// dependencies
	private final MessageDispatcher<Object> msgDispatcher;
	private final JoinOperation joinOperation;
	private final GetPeersRequest contentRequestProvider;
	private final ContentMessage contentMessage;
	private final IncomingContentHandler<Object> incomingContentHandler;
	private final FindValueOperation findValueOperation;
	private final FindNodeHandler findNodeHandler;
	private final PingHandler pingHandler;
	private final StoreHandler storeHandler;
	// private final ForwardHandler forwardHandlerProvider;

	private final Node localNode;
	private final Communicator kadServer;
	private final NodeStorage nodeStorage;
	private final KeyFactory keyFactory;
	private final ExecutorService clientExecutor;
	private final int bucketSize;
	private final TimerTask refreshTask;
	private final BootstrapNodesSaver bootstrapNodesSaver;

	// testing
	private final List<Integer> findNodeHopsHistogram;

	// state
	private final Map<String, MessageDispatcher<?>> dispatcherFromTag = new HashMap<String, MessageDispatcher<?>>();
	private Thread kadServerThread = null;

	protected KadNet(MessageDispatcher<Object> msgDispatcherProvider, JoinOperation joinOperationProvider, //
			GetPeersRequest contentRequestProvider, ContentMessage contentMessageProvider, //
			IncomingContentHandler<Object> incomingContentHandlerProvider, FindValueOperation findValueOperationProvider, //
			FindNodeHandler findNodeHandlerProvider, PingHandler pingHandler, StoreHandler storeHandlerProvider,//
			Node localNode, Communicator kadServer, NodeStorage nodeStorage, //
			KeyFactory keyFactory, ExecutorService clientExecutor, int bucketSize, TimerTask refreshTask,//
			BootstrapNodesSaver bootstrapNodesSaver,// testing
			List<Integer> findNodeHopsHistogram) {

		this.msgDispatcher = msgDispatcherProvider;
		this.joinOperation = joinOperationProvider;
		this.contentRequestProvider = contentRequestProvider;
		this.contentMessage = contentMessageProvider;
		this.incomingContentHandler = incomingContentHandlerProvider;
		this.findValueOperation = findValueOperationProvider;
		this.findNodeHandler = findNodeHandlerProvider;
		this.pingHandler = pingHandler;
		this.storeHandler = storeHandlerProvider;
		// this.forwardHandlerProvider = forwardHandlerProvider;

		this.localNode = localNode;
		this.kadServer = kadServer;
		this.nodeStorage = nodeStorage;
		this.keyFactory = keyFactory;
		this.clientExecutor = clientExecutor;
		this.bucketSize = bucketSize;
		this.refreshTask = refreshTask;
		this.bootstrapNodesSaver = bootstrapNodesSaver;

		// testing
		this.findNodeHopsHistogram = findNodeHopsHistogram;
	}

	@Override
	public void create() throws IOException {
		// bind communicator and register all handlers
		// kadServer.bind();
		pingHandler.register();
		findNodeHandler.register();
		storeHandler.register();
		// forwardHandlerProvider.register();

		nodeStorage.registerIncomingMessageHandler();
		kadServerThread = new Thread(kadServer);
		kadServerThread.start();

		bootstrapNodesSaver.load();
		bootstrapNodesSaver.start();
	}

	@Override
	public void join(Collection<URI> bootstraps) {
		joinOperation.addBootstrap(bootstraps).doJoin();
	}

	@Override
	public List<Node> findNode(Key k) {
		FindValueOperation op = findValueOperation.setKey(k);

		List<Node> result = op.doFindValue();
		findNodeHopsHistogram.add(op.getNrQueried());

		List<Node> $ = new ArrayList<Node>(result);

		if ($.size() > bucketSize)
			$.subList(bucketSize, $.size()).clear();

		// System.out.println(op.getNrQueried());

		return result;
	}

	@Override
	public KeyFactory getKeyFactory() {
		return keyFactory;
	}

	@Override
	public List<Node> getNeighbours() {
		return nodeStorage.getAllNodes();
	}

	@Override
	public Node getLocalNode() {
		return localNode;
	}

	@Override
	public String toString() {
		return localNode.toString() + "\n" + nodeStorage.toString();
	}

	@Override
	public synchronized void register(String tag, MessageHandler handler) {
		MessageDispatcher<?> dispatcher = dispatcherFromTag.get(tag);
		if (dispatcher != null)
			dispatcher.cancel(new CancellationException());

		dispatcher = msgDispatcher.addFilter(new TagMessageFilter(tag)).setConsumable(false)//
				.setCallback(null, incomingContentHandler.setHandler(handler).setTag(tag)).register();

		dispatcherFromTag.put(tag, dispatcher);
	}

	@Override
	public void sendMessage(Node to, String tag, Serializable msg) throws IOException {
		kadServer.send(to, contentMessage.setTag(tag).setContent(msg));
	}

	@Override
	public Future<Serializable> sendRequest(Node to, String tag, Serializable msg) {

		GetPeersRequest contentRequest = contentRequestProvider.setTag(tag).setContent(msg);

		Future<KadMessage> futureSend = msgDispatcher.setConsumable(true)//
				.addFilter(new TypeMessageFilter(GetPeersResponse.class))//
				.addFilter(new IdMessageFilter(contentRequest.getId()))//
				.futureSend(to, contentRequest);

		return new FutureTransformer<KadMessage, Serializable>(futureSend) {
			@Override
			protected Serializable transform(KadMessage msg) throws Throwable {
				return ((GetPeersResponse) msg).getContent();
			}
		};
	}

	@Override
	public <A> void sendRequest(Node to, String tag, Serializable msg, final A attachment, final CompletionHandler<Serializable, A> handler) {
		GetPeersRequest contentRequest = contentRequestProvider.setTag(tag).setContent(msg);

		msgDispatcher.setConsumable(true).addFilter(new TypeMessageFilter(GetPeersResponse.class)).addFilter(new IdMessageFilter(contentRequest.getId())).setCallback(null, new CompletionHandler<KadMessage, Object>() {
			@Override
			public void completed(KadMessage msg, Object nothing) {
				final GetPeersResponse contentResponse = (GetPeersResponse) msg;
				clientExecutor.execute(new Runnable() {
					@Override
					public void run() {
						handler.completed(contentResponse.getContent(), attachment);
					}
				});
			}

			@Override
			public void failed(Throwable exc, Object nothing) {
				handler.failed(exc, attachment);
			}
		}).send(to, contentRequest);
	}

	public static void main(String[] args) throws Exception {
		// Injector injector = Guice.createInjector(new KadNetModule().setProperty("openkad.net.udp.port", "5555"));
		// KeybasedRouting kbr = injector.getInstance(KeybasedRouting.class);
		// kbr.create();
	}

	@Override
	public void shutdown() {
		try {
			bootstrapNodesSaver.saveNow();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		refreshTask.cancel();
		kadServer.shutdown(kadServerThread);
	}
}
