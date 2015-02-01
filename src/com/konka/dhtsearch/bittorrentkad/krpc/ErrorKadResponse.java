package com.konka.dhtsearch.bittorrentkad.krpc;

import com.konka.dhtsearch.Node;

public class ErrorKadResponse extends KadResponse {

	/**
	 */
	private static final long serialVersionUID = 5277034232185175274L;

	protected ErrorKadResponse(String transaction, Node src) {
		super(transaction, src);
	}

	@Override
	public byte[] getBencodeData(Node to) {
		return null;
	}

}
