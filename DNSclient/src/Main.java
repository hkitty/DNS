import java.net.InetAddress; 
import java.net.UnknownHostException; 
		
public class Main { 
	public static void main(String[] args) { 
		if (args.length > 0) { 
			try { 
				InetAddress[] addrArr = InetAddress.getAllByName(args[0]);
				for (int i = 0; i < addrArr.length; ++i) {
					System.out.println(addrArr[i]); 
				}
			}
			catch (UnknownHostException e) { 
				e.printStackTrace();
				System.out.println("Unknown\n"); 
			} 
			} 
		}
}