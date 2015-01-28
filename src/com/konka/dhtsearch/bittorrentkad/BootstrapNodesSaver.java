package com.konka.dhtsearch.bittorrentkad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.bucket.KadBuckets;

/**
 * 开机节点保护
 * 
 * @author konka
 *
 */
public class BootstrapNodesSaver {

	private final KadBuckets kBuckets;
	private final KadNode kadNodeProvider;
	private final File nodesFile;

	BootstrapNodesSaver(KadBuckets kBuckets, KadNode kadNodeProvider, File nodesFile) {

		this.kBuckets = kBuckets;
		this.kadNodeProvider = kadNodeProvider;
		this.nodesFile = nodesFile;
	}

	public void start() {

	}

	/**
	 * 马上保存
	 * 
	 * @throws IOException
	 */
	public void saveNow() throws IOException {
		FileOutputStream fout = null;
		ObjectOutputStream oout = null;
		try {
			fout = new FileOutputStream(nodesFile);
			oout = new ObjectOutputStream(fout);

			oout.writeObject(kBuckets.getAllNodes());

		} finally {
			if (oout != null)
				oout.close();
			if (fout != null)
				fout.close();
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * 从保存的文件中读取
	 * @throws IOException
	 */
	public void load() throws IOException {
		if (nodesFile.length() == 0L)
			return;

		FileInputStream fin = null;
		ObjectInputStream oin = null;
		List<Node> nodes;
		try {
			fin = new FileInputStream(nodesFile);
			oin = new ObjectInputStream(fin);

			nodes = (List<Node>) oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);

		} finally {
			if (oin != null)
				oin.close();
			if (fin != null)
				fin.close();
		}

		for (Node n : nodes) {
			kBuckets.insert(kadNodeProvider .setNode(n));
		}
	}

}
