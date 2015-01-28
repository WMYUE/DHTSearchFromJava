package com.konka.dhtsearch.bittorrentkad.handlers;

import java.util.Arrays;
import java.util.Collection;

import com.konka.dhtsearch.bittorrentkad.cache.KadCache;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.StoreMessage;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.MessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;

/**
 * Handle store messages by inserting the content to the cache
 * @author eyal.kibbar@gmail.com
 *
 */
public class StoreHandler extends AbstractHandler {

	private final KadCache cache;
	
	StoreHandler(
			 MessageDispatcher<Void>  msgDispatcherProvider,
			KadCache cache) {
		super(msgDispatcherProvider);
		this.cache = cache;
	}

	@Override
	public void completed(KadMessage msg, Void attachment) {
		StoreMessage storeMsg = (StoreMessage)msg;
		cache.insert(storeMsg.getKey(), storeMsg.getNodes());
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// should never b here
	}

	@Override
	protected Collection<MessageFilter> getFilters() {
		return Arrays.asList(new MessageFilter[] {
				new TypeMessageFilter(StoreMessage.class)
		});
	}

}
