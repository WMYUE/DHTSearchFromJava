package com.konka.dhtsearch.bittorrentkad.net.filter;

import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;

/**
 * id过滤器，可以屏蔽自己
 * 
 */
public class IdMessageFilter implements MessageFilter {

	private final String transaction;

	public IdMessageFilter(String transaction) {
		this.transaction = transaction;
	}

	@Override
	public boolean shouldHandle(KadMessage m) {
		return m.getTransaction() .equals(transaction);
	}
}
