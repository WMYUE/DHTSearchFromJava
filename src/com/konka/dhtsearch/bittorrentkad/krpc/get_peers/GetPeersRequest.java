package com.konka.dhtsearch.bittorrentkad.krpc.get_peers;

import java.io.Serializable;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;

/**
 * A message containing arbitrary data to be used by the KeybasedRouting.sendRequest methods
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class GetPeersRequest extends KadRequest {

	private static final long serialVersionUID = 918433377540165654L;

	private String tag;
	private Serializable content;

	GetPeersRequest(long id, Node src) {

		super(id, src);
	}

	public String getTag() {
		return tag;
	}

	public Serializable getContent() {
		return content;
	}

	public GetPeersRequest setContent(Serializable content) {
		this.content = content;
		return this;
	}

	public GetPeersRequest setTag(String tag) {
		this.tag = tag;
		return this;
	}

	@Override
	public GetPeersResponse generateResponse(Node localNode) {
		return new GetPeersResponse(getId(), localNode);
	}

}
