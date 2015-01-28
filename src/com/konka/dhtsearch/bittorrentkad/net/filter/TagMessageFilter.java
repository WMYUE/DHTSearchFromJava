package com.konka.dhtsearch.bittorrentkad.net.filter;

import com.konka.dhtsearch.bittorrentkad.krpc.ContentMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersRequest;

/**
 * Reject all message with different tag than the given tag
 * @author eyal.kibbar@gmail.com
 *
 */
public class TagMessageFilter implements MessageFilter {

	private final String tag;
	
	
	public TagMessageFilter(String tag) {
		this.tag = tag;
	}
	
	@Override
	public boolean shouldHandle(KadMessage m) {
		String tag = null;
		if (m instanceof GetPeersRequest)
			tag = ((GetPeersRequest)m).getTag();
		else if (m instanceof ContentMessage)
			tag = ((ContentMessage)m).getTag();
		else
			return false;
		
		return this.tag.equals(tag);
	}

}
