package org.konkakjb.text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.RandomKeyFactory;
import com.konka.dhtsearch.bencode.BDecoder;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.bittorrentkad.KadNode;

public class SearchText {
	public static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);
	public static byte[] File2byte(String filePath)  
    {  
        byte[] buffer = null;  
        try  
        {  
            File file = new File(filePath);  
            FileInputStream fis = new FileInputStream(file);  
            ByteArrayOutputStream bos = new ByteArrayOutputStream();  
            byte[] b = new byte[1024];  
            int n;  
            while ((n = fis.read(b)) != -1)  
            {  
                bos.write(b, 0, n);  
            }  
            fis.close();  
            bos.close();  
            buffer = bos.toByteArray();  
        }  
        catch (FileNotFoundException e)  
        {  
            e.printStackTrace();  
        }  
        catch (IOException e)  
        {  
            e.printStackTrace();  
        }  
        return buffer;  
    } 
	public static void main(String[] args) {
		KadNet kadNet = null;
		try {
			AppManager.init();// 1---
			// File nodesFile = new File(SearchText.class.getResource("/").getPath()); // 2---
			// BootstrapNodesSaver bootstrapNodesSaver = new BootstrapNodesSaver(nodesFile);// 3---
			// kadNet = new KadNet(bootstrapNodesSaver);
			kadNet = new KadNet(null);
			kadNet.create();
//			doth(kadNet);
		} catch (Exception e) {
			e.printStackTrace();
			if (kadNet != null) {
				kadNet.shutdown();
			}
		}
		try {
			File file=new File("D:/aaa.torrent");
//			BDecoder bDecoder=new BDecoder(new FileInputStream(file));
//			Object object=bDecoder.decodeMap();
//			System.out.println(object);
//			ByteArrayInputStream arrayInputStream=new 
//			new FileInputStream(file).
//			file.
//			BEncodedInputStream.bdecode(arg0)
//			BMap bMap = (BMap) BEncodedInputStream.bdecode(pkt
//					.getData());
			BEncodedInputStream bEncodedInputStream=new BEncodedInputStream(new FileInputStream(file));
			Object object2=bEncodedInputStream.readElement();
			System.out.println(object2);
			bEncodedInputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void doth(KadNet kadNet) throws Exception {
		InetAddress[] inetAddresss = { //
		InetAddress.getByName("router.bittorrent.com"), //
				InetAddress.getByName("dht.transmissionbt.com"), //
				InetAddress.getByName("router.utorrent.com"), //
//				Inet4Address.getByName("127.0.0.1") //
		};
		try {
			for (InetAddress inetAddress : inetAddresss) {
				Key key = new RandomKeyFactory(20, new Random(), "SHA-1").generate();
				Node localNode = new Node(key).setInetAddress(inetAddress).setPoint(6881);
				kadNet.getKadBuckets().insert(new KadNode().setNode(localNode).setNodeWasContacted());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	private static void sendFindNode(final Node localNode,//
//			final FindNodeRequest findNodeResponse, KadNet kadNet) {
//		
//		Timer timer = new Timer();
//		KadSendMsgServer kadServer = kadNet.getKadSendMsgServer();
//		// try {
//		// kadServer.send(localNode, findNodeResponse);
//		// } catch (IOException e1) {
//		// e1.printStackTrace();
//		// }
//		final MessageDispatcher dispatcher = new MessageDispatcher(timer, kadServer, findNodeResponse.getTransaction());
//		dispatcher.setConsumable(true)//
//				// .addFilter(new IdMessageFilter(findNodeResponse.getTransaction()))// 只接受的类型
//				// .addFilter(new TypeMessageFilter(FindNodeResponse.class))//
//				.setCallback(null, new CompletionHandler<KadMessage, String>() {
//					@Override
//					public void completed(KadMessage msg, String nothing) {
////						System.out.println("收到请求的响应" + msg);
//					}
//
//					@Override
//					public void failed(Throwable exc, String nothing) {
//
//					}
//				});
//		try {
//			executor.execute(new Runnable() {
//				@Override
//				public void run() {
//					dispatcher.send(findNodeResponse);
//					
//				}
//			});
//			// dispatcher.f
//			// ExecutorCompletionService d;
//			// d.
//			// executor.submit(task);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
