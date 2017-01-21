import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		String userSearch = null;
		String userDNS = null;
		
		int userType = 0; 
		
		String search = "www.vk.com";
		String DNS = "8.8.8.8"; //192.112.36.4, 62.233.233.233
		
		int type = 1;
		
		for(String arg : args)
		{
			if(arg.startsWith("-d="))
				userSearch = arg.substring(3);
			else if(arg.startsWith("-ns="))
				userDNS = arg.substring(4);
			else if(arg.startsWith("-t="))
				userType = Integer.parseInt(arg.substring(3));
		}

		if(userSearch == null)
		{
			System.out.println("Using defaut gateway: " + search);
		}
		else
		{
			search = userSearch;
			System.out.println("Gateway for search: " + search);
		}
		if(userDNS == null)
		{
			System.out.println("Using defulf DNS: " + DNS);	
		}
		else
		{
			DNS = userDNS;
			System.out.println("DNS: " + search);
		}
		if(userType == 0)
		{
			System.out.println("Using defaul type: " + type);
		}
		else
		{
			type = userType;
			System.out.println("Type: " + type);
		}
		

		if(type < 1 || type >= Record.TYPES.length)
		{
			System.out.println("Wrong type. Please, use: 1 (A), 2 (NS), 5 (CNAME), 6 (SOA), 15 (MX), 16 (TXT)");
			System.exit(1);
		}

		DatagramSocket socket = new DatagramSocket(12345);
		socket.setSoTimeout(5000);
		Packet.socket = socket;

		Random random = new Random();
		while(true)
		{
			System.out.println("-------------------------------------------------------");
			Packet answer = get(search, DNS, type);
			System.out.println("OPCode: " + answer.OPCODE + ", Answers: " + answer.ANCount + ", Authority Records: " + answer.NSCount);
			boolean gotAnswer = false;

			if(answer.OPCODE != 0)
			{
				System.out.println("Error:");
				switch(answer.OPCODE)
				{
					case (byte) 1:
						System.out.println("Format Error");
						break;
					case (byte) 2:
						System.out.println("Server Failure");
						break;
					case (byte) 3:
						System.out.println("Name Error");
						break;
					case (byte) 4:
						System.out.println("Not Implemented");
						break;
					case (byte) 5:
						System.out.println("Refused");
						break;
					default:
						System.out.println("Not supported. ID: " + answer.OPCODE);
						break;
				}
				break;
			}
			if(answer.ANCount > 0)
			{
				System.out.println("Answers:");
				for(int i = 0; i < answer.ANCount; i++)
				{
					Record record = answer.ANList[i];
					answer.ptr = record.RData;
					if(record.QType == type)
					{
						gotAnswer = true;
					}
					switch(record.QType)
					{
						case (byte) 1: // A
							InetAddress address = InetAddress.getByAddress(new byte[]
                             { answer.response[answer.ptr++], answer.response[answer.ptr++], answer.response[answer.ptr++], answer.response[answer.ptr++] });
							System.out.println("Typ: A, Domain: " + record.domain + ", IP: " + address.getHostAddress());
							break;
						case (byte) 2: // NS
							System.out.println("Typ: NS, Domain: " + record.domain + ", Domain NS: " + answer.getStringFromPoint());
							break;
						case (byte) 5: // CNAME
							System.out.println("Typ: CNAME, Domain: " + record.domain + ", The actual domain: " + answer.getStringFromPoint());
							break;
						case (byte) 6: // SOA
							System.out.println("Typ: SOA, Domain: " + record.domain + ", Domain NS: " + answer.getStringFromPoint() + ", Email: " + answer.getStringFromPoint());
							break;
						case (byte) 15: // MX
							answer.ptr += 2;
							System.out.println("Typ: MX, Domain: " + record.domain + ", Address: " + answer.getStringFromPoint());
							break;
						case (byte) 16: // TXT
							System.out.print("Typ: TXT, Domain: " + record.domain + ", Text: ");
							for(int tt = 0; tt < answer.response[answer.ptr]; tt++)
							{
								System.out.print((char) answer.response[answer.ptr + tt + 1]);
							}
							System.out.println();
							break;
						default:
							System.out.println("Received the answer: " + record.QType);
							break;
					
					}
				}
				if(gotAnswer)
				{
					break;
				}
			}
			if(!gotAnswer && answer.NSCount > 0)
			{

				System.out.println("NS:");
				ArrayList<String> dnsy = new ArrayList<String>();
				for(int i = 0; i < answer.NSCount; i++)
				{
					Record record = answer.NSList[i];
					if(record.QType == (byte) 2)
					{
						answer.ptr = record.RData;
						String otherDNS = answer.getStringFromPoint();
						if(!otherDNS.equals(DNS))
						{
							dnsy.add(otherDNS);	
						}
						System.out.println("Domain: " + record.domain + ", NS: " + otherDNS);
					}
					else if(record.QType == (byte) 6)
					{
						answer.ptr = record.RData;
						String otherDNS = answer.getStringFromPoint();
						if((type == 2 || type == 6) && search.endsWith(record.domain))
						{
							System.out.println("Type: SOA, Domain: " + record.domain + ", NS: " + otherDNS + ", email: " + answer.getStringFromPoint());
							System.exit(1);
						}
						if(!otherDNS.equals(DNS))
						{
							dnsy.add(otherDNS);	
						}
						System.out.println("Domain: " + record.domain + ", SOA: " + otherDNS);
					}
					else
					{
						System.err.println("Info about: " + record.domain + ", with type: " + record.QType + ", supported only NS (2).");
					}
				}
				if(dnsy.size() == 0)
				{
					System.out.println("Your search did not match.");
					break;
				}
				DNS = dnsy.get(random.nextInt(dnsy.size()));
				System.out.println("You have chosen a new DNS: " + DNS);
			}
			else
			{
				System.out.println("Your search did not match.");
				break;
			}
		}
		socket.close();
	}

	public static Packet get(String domain, String fromDNS, int type) throws UnknownHostException, IOException
	{
		Packet packet = new Packet(domain, type);
		boolean reponseReceived = false;
		for(int i = 0; i < 3; i++)
		{
			System.out.println("Searching info about " + domain + " on the " + fromDNS + ", with type " + Record.TYPES[type] + ".");
			if(packet.query(InetAddress.getByName(fromDNS)))
			{
				reponseReceived = true;
				break;
			}
			else
			{
				System.out.println("No answer 5 seconds...");
			}
		}
		if(reponseReceived)
		{
			packet.parseResponse();
		}
		else
		{
			System.out.println("Could not get a response from the DNS server " + fromDNS);
			System.exit(1);
		}
		return packet;
	}
}
