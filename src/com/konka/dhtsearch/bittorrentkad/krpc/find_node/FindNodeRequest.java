package com.konka.dhtsearch.bittorrentkad.krpc.find_node;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;

/**
 * A findNode request as defined in the kademlia protocol
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class FindNodeRequest extends KadRequest {

	private static final long serialVersionUID = -7084922793331210968L;
	private Key key;
	private boolean searchCache;

	public FindNodeRequest(String transaction, Node src) {
		super(transaction, src);
	}

	/**
	 * 
	 * @return the key we are searching
	 */
	public Key getKey() {
		return key;
	}

	public FindNodeRequest setKey(Key key) {
		this.key = key;
		return this;
	}

	@Override
	public FindNodeResponse generateResponse(Node localNode) {
		return new FindNodeResponse(getTransaction(), localNode);
	}

	public FindNodeRequest setSearchCache(boolean searchCache) {
		this.searchCache = searchCache;
		return this;
	}

	public boolean shouldSearchCache() {
		return searchCache;
	}

}
