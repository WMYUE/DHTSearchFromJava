package com.konka.dhtsearch.bittorrentkad.net.filter;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;

/**
 * Rejects all messages from src other than the given src
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class SrcExcluderMessageFilter implements MessageFilter {

	private final Node src;
	
	public SrcExcluderMessageFilter(Node src) {
		this.src = src;
	}
	
	
	
	@Override
	public boolean shouldHandle(KadMessage m) {
		return !src.equals(m.getSrc());
	}
	
}
