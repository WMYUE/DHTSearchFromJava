package com.konka.dhtsearch.db.models;

public class DhtInfo {
	private long id;
	private String info_hash;
	private String peerIp;
	private String fileName;
	private String torrentFilePath;
	private String filesize;
	private String createTime;// 种子创建时间
	private String fileList;// 多文件时候的file列表
	private long lastRequestsTime;// 最后请求时间
	private int analysised;// 是否解析了，1 ok 0没有解析
	private String tag;// 标识

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getInfo_hash() {
		return info_hash;
	}

	public void setInfo_hash(String info_hash) {
		this.info_hash = info_hash;
	}

	public String getPeerIp() {
		return peerIp;
	}

	public void setPeerIp(String peerIp) {
		this.peerIp = peerIp;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTorrentFilePath() {
		return torrentFilePath;
	}

	public void setTorrentFilePath(String torrentFilePath) {
		this.torrentFilePath = torrentFilePath;
	}

	public String getFilesize() {
		return filesize;
	}

	public void setFilesize(String filesize) {
		this.filesize = filesize;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getFileList() {
		return fileList;
	}

	public void setFileList(String fileList) {
		this.fileList = fileList;
	}

	public long getLastRequestsTime() {
		return lastRequestsTime;
	}

	public void setLastRequestsTime(long lastRequestsTime) {
		this.lastRequestsTime = lastRequestsTime;
	}

	public int getAnalysised() {
		return analysised;
	}

	public void setAnalysised(int analysised) {
		this.analysised = analysised;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
