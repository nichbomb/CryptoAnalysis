package demo

import javax.crypto.Cipher

class DemoClass {

	companion object {
		@JvmStatic fun main(args: Array<String>) {
			System.out.println("Hello World!");
			var instance: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
		}
	}
	
	fun methodA() {
		
	}
	
	class B {
		fun methodB() {
			var instance: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
		}
	}
}

class ClassA {
	
}