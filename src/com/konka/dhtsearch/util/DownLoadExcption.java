package com.konka.dhtsearch.util;

public class DownLoadExcption extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4424637432835502134L;
	public DownLoadExcption() {
		super();
	}

	public DownLoadExcption(String msg) {
        super(msg);
    }

	public DownLoadExcption(Exception exception) {
		super(exception);
	}
}
