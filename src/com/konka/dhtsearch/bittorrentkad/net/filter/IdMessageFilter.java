package com.konka.dhtsearch.bittorrentkad.net.filter;

import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;

/**
 * id过滤器，可以屏蔽自己
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
