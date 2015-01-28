package com.konka.dhtsearch.bittorrentkad.msg;

import com.konka.dhtsearch.Node;

/**
 * A ping response as defined in the kademlia protocol
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class PingResponse extends KadResponse {

	private static final long serialVersionUID = -5054944878934710372L;

	PingResponse(long id, Node src) {
		super(id, src);
	}

}
