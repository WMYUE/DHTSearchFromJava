package com.konka.dhtsearch.bittorrentkad.krpc.ping;

import org.yaircc.torrent.bencoding.BMap;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadResponse;

/**
 * A ping response as defined in the kademlia protocol
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class PingResponse extends KadResponse {

	private static final long serialVersionUID = -5054944878934710372L;

	public PingResponse(String transaction, Node src) {
		super(transaction, src);
	}

	@Override
	public byte[] getBencodeData(Node to) {
		// TODO Auto-generated method stub
		return null;
	}

 

}
