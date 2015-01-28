package com.konka.dhtsearch.bittorrentkad.handlers;

import java.util.Collection;

import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.MessageFilter;

/**
 * Base class for all incoming message handlers
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public abstract class AbstractHandler implements CompletionHandler<KadMessage, Void> {

	private final MessageDispatcher<Void> msgDispatcherProvider;

	protected AbstractHandler(MessageDispatcher<Void> msgDispatcherProvider) {
		this.msgDispatcherProvider = msgDispatcherProvider;
	}

	/**
	 * @return all the filters associated with this handler
	 */
	protected abstract Collection<MessageFilter> getFilters();

	/**
	 * Register this handler for start receiving messages
	 */
	public void register() {
		MessageDispatcher<Void> dispatcher = msgDispatcherProvider;

		for (MessageFilter filter : getFilters()) {
			dispatcher.addFilter(filter);
		}

		dispatcher.setConsumable(false).setCallback(null, this).register();
	}
}
