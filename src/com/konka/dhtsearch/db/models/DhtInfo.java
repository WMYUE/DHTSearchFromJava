package com.konka.dhtsearch.db.models;

import java.util.List;

import com.konka.dhtsearch.db.mongodb.orm1.MongoCollection;
import com.konka.dhtsearch.db.mysql.DhtInfoStateCode;
import com.konka.dhtsearch.parser.MultiFile;

@MongoCollection
public class DhtInfo {
	private long id;
	private String info_hash;
	private String peerIp;
	private String fileName;
	private String torrentFilePath;
	private long fileSize;
	private long createTime;// 种子创建时间
	private List<MultiFile> fileList;
//	private String fileList;// 多文件时候的file列表
	private long lastRequestsTime;// 最后请求时间
	/**
	 * @category 没有下载。
	 * @category 下载失败。
	 * @category 下载成功，解析失败
	 * @category 下载成功，解析成功
	 */
	private int analysised = DhtInfoStateCode.NO_DOWNLOAD;
	private String tag = "";// 标识

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
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

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public List<MultiFile> getFileList() {
		return fileList;
	}

	public void setFileList(List<MultiFile> fileList) {
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
