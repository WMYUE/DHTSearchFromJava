package com.konka.dhtsearch.bittorrentkad.handlers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingResponse;
import com.konka.dhtsearch.bittorrentkad.net.Communicator;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.MessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;

/**
 * Handles ping requests by sending back a ping response
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class PingHandler extends AbstractHandler {

	private final Communicator kadServer;
	private final Node localNode;
	private final AtomicInteger nrIncomingPings;

	PingHandler(MessageDispatcher<Void> msgDispatcherProvider, Communicator kadServer, Node localNode,//
			AtomicInteger nrIncomingPings) {
		super(msgDispatcherProvider);
		this.kadServer = kadServer;
		this.localNode = localNode;
		this.nrIncomingPings = nrIncomingPings;
	}

	@Override
	public void completed(KadMessage msg, Void attachment) {
		nrIncomingPings.incrementAndGet();
		PingResponse pingResponse = ((PingRequest) msg).generateResponse(localNode);

		try {
			kadServer.send(msg.getSrc(), pingResponse);
		} catch (IOException e) {
			// nothing to do
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// should never b here
	}

	@Override
	protected Collection<MessageFilter> getFilters() {
		return Arrays.asList(new MessageFilter[] { new TypeMessageFilter(PingRequest.class) });
	}

}
