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
import com.konka.dhtsearch.bittorrentkad.msg.ContentMessage;
import com.konka.dhtsearch.bittorrentkad.msg.ContentRequest;
import com.konka.dhtsearch.bittorrentkad.msg.ContentResponse;
import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;
import com.konka.dhtsearch.bittorrentkad.net.Communicator;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.IdMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TagMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;
import com.konka.dhtsearch.bittorrentkad.op.FindValueOperation;
import com.konka.dhtsearch.bittorrentkad.op.JoinOperation;

public class KadNet implements KeybasedRouting {

	// dependencies
	private final MessageDispatcher<Object> msgDispatcherProvider;
	private final JoinOperation joinOperationProvider;
	private final ContentRequest contentRequestProvider;
	private final ContentMessage contentMessageProvider;
	private final IncomingContentHandler<Object> incomingContentHandlerProvider;
	private final FindValueOperation findValueOperationProvider;
	private final FindNodeHandler findNodeHandlerProvider;
	private final PingHandler pingHandler;
	private final StoreHandler storeHandlerProvider;
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
			ContentRequest contentRequestProvider, ContentMessage contentMessageProvider, //
			IncomingContentHandler<Object> incomingContentHandlerProvider, FindValueOperation findValueOperationProvider, //
			FindNodeHandler findNodeHandlerProvider, PingHandler pingHandler, StoreHandler storeHandlerProvider,//
			Node localNode, Communicator kadServer, NodeStorage nodeStorage, //
			KeyFactory keyFactory, ExecutorService clientExecutor, int bucketSize, TimerTask refreshTask,//
			BootstrapNodesSaver bootstrapNodesSaver,// testing
			List<Integer> findNodeHopsHistogram) {

		this.msgDispatcherProvider = msgDispatcherProvider;
		this.joinOperationProvider = joinOperationProvider;
		this.contentRequestProvider = contentRequestProvider;
		this.contentMessageProvider = contentMessageProvider;
		this.incomingContentHandlerProvider = incomingContentHandlerProvider;
		this.findValueOperationProvider = findValueOperationProvider;
		this.findNodeHandlerProvider = findNodeHandlerProvider;
		this.pingHandler = pingHandler;
		this.storeHandlerProvider = storeHandlerProvider;
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
		findNodeHandlerProvider.register();
		storeHandlerProvider.register();
//		forwardHandlerProvider.register();

		nodeStorage.registerIncomingMessageHandler();
		kadServerThread = new Thread(kadServer);
		kadServerThread.start();

		bootstrapNodesSaver.load();
		bootstrapNodesSaver.start();
	}

	@Override
	public void join(Collection<URI> bootstraps) {
		joinOperationProvider.addBootstrap(bootstraps).doJoin();
	}

	@Override
	public List<Node> findNode(Key k) {
		FindValueOperation op = findValueOperationProvider.setKey(k);

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

		dispatcher = msgDispatcherProvider.addFilter(new TagMessageFilter(tag)).setConsumable(false)//
				.setCallback(null, incomingContentHandlerProvider.setHandler(handler).setTag(tag)).register();

		dispatcherFromTag.put(tag, dispatcher);
	}

	@Override
	public void sendMessage(Node to, String tag, Serializable msg) throws IOException {
		kadServer.send(to, contentMessageProvider.setTag(tag).setContent(msg));
	}

	@Override
	public Future<Serializable> sendRequest(Node to, String tag, Serializable msg) {

		ContentRequest contentRequest = contentRequestProvider.setTag(tag).setContent(msg);

		Future<KadMessage> futureSend = msgDispatcherProvider.setConsumable(true)//
				.addFilter(new TypeMessageFilter(ContentResponse.class)).addFilter(new IdMessageFilter(contentRequest.getId())).futureSend(to, contentRequest);

		return new FutureTransformer<KadMessage, Serializable>(futureSend) {
			@Override
			protected Serializable transform(KadMessage msg) throws Throwable {
				return ((ContentResponse) msg).getContent();
			}
		};
	}

	@Override
	public <A> void sendRequest(Node to, String tag, Serializable msg, final A attachment, final CompletionHandler<Serializable, A> handler) {
		ContentRequest contentRequest = contentRequestProvider.setTag(tag).setContent(msg);

		msgDispatcherProvider.setConsumable(true).addFilter(new TypeMessageFilter(ContentResponse.class)).addFilter(new IdMessageFilter(contentRequest.getId())).setCallback(null, new CompletionHandler<KadMessage, Object>() {
			@Override
			public void completed(KadMessage msg, Object nothing) {
				final ContentResponse contentResponse = (ContentResponse) msg;
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
