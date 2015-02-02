package com.konka.dhtsearch.bittorrentkad.krpc;

import java.util.ArrayList;

import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.HashBMap;

import com.konka.dhtsearch.Node;

public class ErrorKadResponse extends KadResponse {

	/**
	 */
	private static final long serialVersionUID = 5277034232185175274L;
	public ErrorKadResponse(String transaction, Node src) {
		super(transaction, src);
	}

	@Override
	public byte[] getBencodeData(Node to) {
		BMap bMap = new HashBMap();
		bMap.put(TRANSACTION, transaction);
		bMap.put("y", "e");
		
		ArrayList<Object> bList= new ArrayList<Object>();
		bList.add(202);
		bList.add("Server Error");
		bMap.put("e",bList);
 
		// ----------------------------------
		return BEncodedOutputStream.bencode(bMap);
	}

}
