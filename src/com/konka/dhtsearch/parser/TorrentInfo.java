package com.konka.dhtsearch.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.yaircc.torrent.bencoding.BDecodingException;
import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.BTypeException;

import com.konka.dhtsearch.util.StringUtil;

public class TorrentInfo implements TorrentConstantKey {
	private String name;// 文件名称
	private long filelenth;// 文件大小 单位 byte
	private long creattime;// 创建时间 creation date
	private String encoding = null;// 编码方式 utf-8
	private List<MultiFile> multiFiles;
	private boolean singerFile=true;// 是否是单文件 如果是多文件，文件放假multiFiles中

	public boolean isSingerFile() {
		return singerFile;
	}

	public TorrentInfo(InputStream in) {
		BEncodedInputStream bEncodedInputStream = new BEncodedInputStream(in);
		try {
			parser(bEncodedInputStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bEncodedInputStream != null) {
					bEncodedInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void parser(BEncodedInputStream bEncodedInputStream) throws IOException, BDecodingException, BTypeException {
		BMap bMap = (BMap) bEncodedInputStream.readElement();
		if (bMap.containsKey(ENCODING)) {
			encoding = bMap.getString(ENCODING);
			System.out.println("encoding======" + encoding);
		}
		if (bMap.containsKey(CREATION_DATE)) {
			creattime = bMap.getLong(CREATION_DATE);
			System.out.println("creattime=====" + creattime);
		}
		if (bMap.containsKey(INFO)) {
			BMap infoMap = bMap.getMap(INFO);
			if (infoMap != null) {
				if (!StringUtil.isEmpty(encoding) && infoMap.containsKey(NAME)) {
					byte[] namearray = (byte[]) infoMap.get(NAME);
					name = new String(namearray, encoding);
				} else {
					if (infoMap.containsKey(NAME_UTF_8)) {
						byte[] dd = (byte[]) infoMap.get(NAME_UTF_8);
						name = new String(dd, UTF_8);
					}
				}
				System.out.println("name=" + name);

				if (infoMap.containsKey(LENGTH)) {
					filelenth = infoMap.getLong(LENGTH);
					System.out.println("filelenth=" + filelenth);
				}

				if (infoMap.containsKey(FILES)) {
					List<Object> filesMap = infoMap.getList(FILES);
					multiFiles = new ArrayList<MultiFile>(filesMap.size());
					MultiFile multiFile;
					for (Object multiFileobObject : filesMap) {
						BMap multiFilemap = (BMap) multiFileobObject;
						multiFile = new MultiFile();
						if (!StringUtil.isEmpty(encoding) && multiFilemap.containsKey(PATH)) {
							List<byte[]> pathListbytearray = (List<byte[]>) multiFilemap.get(PATH);
							for (byte[] pathbytearray : pathListbytearray) {
								String path = new String(pathbytearray, encoding);
								multiFile.setPath(path);
							}
						} else {
							if (multiFilemap.containsKey(PATH_UTF_8)) {
								List<byte[]> utf8_Path_Bytes_List = (List<byte[]>) multiFilemap.get(PATH_UTF_8);
								for (byte[] utf8_Path_Bytes : utf8_Path_Bytes_List) {
									String path = new String(utf8_Path_Bytes, UTF_8);
									multiFile.setPath(path);
								}
							}
						}
						if (multiFilemap.containsKey(LENGTH)) {
							long length = multiFilemap.getLong(LENGTH);
							multiFile.setSingleFileLength(length);
							System.out.println("path=" + length);
						}
						multiFiles.add(multiFile);
					}
					singerFile=false;
				}

			}
		}

	}

	public TorrentInfo(String filePath) throws FileNotFoundException {
		this(new File(filePath));
	}

	public TorrentInfo(File file) throws FileNotFoundException {
		this(new FileInputStream(file));
	}

}
