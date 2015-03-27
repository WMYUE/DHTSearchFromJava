package org.konkakjb.text;

public class Text3 {
	// public static void main(String[] args) {
	//
	// for (int i = 0; i <= 65535; i++) {
	// System.out.print((char) i + "  ");
	// if (i % 10 == 0)
	// System.out.println();
	// }
	// }
	public static void main(String[] args) {
		int a = (int) (4 * Math.pow(16, 3) + 14 * Math.pow(16, 2)); // 汉字ASCII码值
		int b = (int) (9 * Math.pow(16, 3) + 15 * Math.pow(16, 2) + 10 * Math.pow(16, 1)) + 5; // 汉字ASCII码值
		int j = 0;
		for (int i = a; i <= b; i++) {
			j++;
			System.out.print((char) i + "\t"); // ASCII码转换字符（汉字）
			if (j % 10 == 0) {
				System.out.println();
				j = 0;
			}
		}
		// new StringBuilder().append(c)
		System.out.println(a);
		System.out.println(b);
	}

	public static void text() {
		// http://www.btcherry.com/search?keyword=%E9%B8%AD%E7%8E%8B
		
	}
}
