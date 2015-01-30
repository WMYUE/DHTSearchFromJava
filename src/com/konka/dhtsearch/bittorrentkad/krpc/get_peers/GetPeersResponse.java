package com.konka.dhtsearch.bittorrentkad.krpc.get_peers;

import java.io.Serializable;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadResponse;

/**
 * A message containing arbitrary data to be used by the KeybasedRouting.sendRequest methods
 * @author eyal.kibbar@gmail.com
 *
 */
public class GetPeersResponse extends KadResponse {

	private static final long serialVersionUID = -4479208136049358778L;

	private Serializable content;
	
	GetPeersResponse(String transaction, Node src) {
		super(transaction, src);
	}

	public Serializable getContent() {
		return content;
	}
	
	public GetPeersResponse setContent(Serializable content) {
		this.content = content;
		return this;
	}

	@Override
	public byte[] getBencodeData(Node to) {
		// TODO Auto-generated method stub
		return null;
	}

 
}
