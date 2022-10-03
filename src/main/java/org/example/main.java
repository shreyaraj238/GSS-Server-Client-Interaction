package org.example;

public class main {
	public static void main(String args[]) {
        try {
            new serverGSS().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
