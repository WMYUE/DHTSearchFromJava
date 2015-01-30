package com.konka.dhtsearch.bittorrentkad.krpc;

import org.yaircc.torrent.bencoding.BMap;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.pojo.Find_NodeInfo;

/**
 * Base class for all responses
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public abstract class KadResponse extends KadMessage {

	private static final long serialVersionUID = 5247239397467830857L;

	protected KadResponse(String transaction, Node src) {
		super(transaction, src);
	}

//	protected abstract BMap getResponseValue();

//	public Find_NodeInfo getFind_NodeInfo(){
//		
//	}
 }
