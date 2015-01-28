package com.konka.dhtsearch.bittorrentkad.net.filter;

import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;

/**
 * Rejects all messages with id different from the given ID
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class IdMessageFilter implements MessageFilter {
	
	private final long id;
	
	public IdMessageFilter(long id) {
		this.id = id;
	}

	@Override
	public boolean shouldHandle(KadMessage m) {
		return m.getId() == id;
	}
}
