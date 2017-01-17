import java.net.InetAddress; 
import java.net.UnknownHostException; 
		
public class Main { 
	public static void main(String[] args) { 
		if (args.length > 0) { 
			try { 
				InetAddress addrArr = InetAddress.getByName("www.vk.com"); 
				//InetAddress[] addrArr = "vk.com";
				//for (int i = 0; i < addrArr.length; ++i) {
					System.out.println(addrArr); 
				//}
			}
			catch (UnknownHostException e) { 
				e.printStackTrace();
				System.out.println("Unknown\n"); 
			} 
			} 
		}
}