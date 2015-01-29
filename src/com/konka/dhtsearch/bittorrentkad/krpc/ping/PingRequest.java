package com.konka.dhtsearch.bittorrentkad.krpc.ping;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;

/**
 * A ping request as defined in the kademlia protocol
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class PingRequest extends KadRequest {

	private static final long serialVersionUID = 4646089493549742900L;

	public PingRequest(String transaction, Node src) {
		super(transaction, src);
	}

	@Override
	public PingResponse generateResponse(Node localNode) {
		return new PingResponse(getTransaction(), localNode);
	}

}
