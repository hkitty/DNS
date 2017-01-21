import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;


public class Packet
{
	static int X = 0;
	
	public static final boolean QR_QUERY = false;
	public static final boolean QR_RESPONSE = true;

	public static final boolean AA_NO = false;
	public static final boolean AA_YES = true;

	public static final boolean TC_NO = false;
	public static final boolean TC_YES = true;

	public static final boolean RD_NO = false;
	public static final boolean RD_YES = true;

	public static final boolean RA_NO = false;
	public static final boolean RA_YES = true;

	String domain;
	
	int QType = 1;
	int QClass = 1;
	
	public static DatagramSocket socket;
	
	byte[] response;
	
	String responseString = "";
	
	int ptr = 0;
	int ID = 0;
	int OPCODE = 0;
	
	boolean QR = QR_QUERY;
	boolean AA = AA_NO;
	boolean TC = TC_NO;
	boolean RD = RD_NO;
	boolean RA = RA_NO;
	
	int RCode = 0;
	int QDCount = 0;
	int ANCount = 0;
	int NSCount = 0;
	int ARCount = 0;

	Record[] ANList;
	Record[] NSList;

	Packet(String domain, int type)
	{
		this.domain = domain;
		this.QType = type;
	}

	boolean query(InetAddress dnsServer) throws IOException
	{
		byte[] DNSheader = { (byte) 10110100, (byte) 11001010, (byte) 0000000, (byte) 00000000,
				(byte) 00000000, (byte) 00000001, (byte) 00000000, (byte) 00000000,
				(byte) 00000000, (byte) 00000000, (byte) 00000000, (byte) 00000000 };
		byte[] searchBytes = getDomainBytes(this.domain);
		byte[] typeClass = new byte[]
                { (byte) 0, (byte) QType, (byte) 00000000, (byte) 00000001 };
		byte[] send = new byte[DNSheader.length + searchBytes.length + typeClass.length];

		System.arraycopy(DNSheader, 0, send, 0, DNSheader.length);
		System.arraycopy(searchBytes, 0, send, DNSheader.length, searchBytes.length);
		System.arraycopy(typeClass, 0, send, DNSheader.length+searchBytes.length, typeClass.length);
		
		response = new byte[4096];
		
		DatagramPacket sender = new DatagramPacket(send, DNSheader.length + searchBytes.length + typeClass.length, dnsServer, 53);
		DatagramPacket receiver = new DatagramPacket(response, 4096, dnsServer, 53);
		
		socket.send(sender);
		
		try
		{
			socket.receive(receiver);
		}
		catch(SocketTimeoutException e)
		{
			return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	public void parseResponse()
	{
		ID = response[0] * 256 + response[1];

		if((response[2] & (1 << 7)) != 0)
			QR = QR_RESPONSE;
		else
			QR = QR_QUERY;

		OPCODE = 0;
		
		if((response[2] & (1 << 6)) != 0)
			OPCODE += 8;
		if((response[2] & (1 << 5)) != 0)
			OPCODE += 4;
		if((response[2] & (1 << 4)) != 0)
			OPCODE += 2;
		if((response[2] & (1 << 3)) != 0)
			OPCODE += 1;

		if((response[2] & (1 << 2)) != 0)
			AA = AA_YES;
		else
			AA = AA_NO;

		if((response[2] & (1 << 1)) != 0)
			TC = TC_YES;
		else
			TC = TC_NO;

		if((response[2] & (1 << 0)) != 0)
			RD = RD_YES;
		else
			RD = RD_NO;

		if((response[3] & (1 << 7)) != 0)
			RA = RA_YES;
		else
			RA = RA_NO;

		RCode = 0; 
		
		if((response[3] & (1 << 3)) != 0)
			RCode += 8;
		if((response[3] & (1 << 2)) != 0)
			RCode += 4;
		if((response[3] & (1 << 1)) != 0)
			RCode += 2;
		if((response[3] & (1 << 0)) != 0)
			RCode += 1;

		QDCount = response[4] * 256 + response[5];

		ANCount = response[6] * 256 + response[7];

		NSCount = response[8] * 256 + response[9];

		ARCount = response[10] * 256 + response[11];

		ANList = new Record[ANCount];

		NSList = new Record[NSCount];

		ptr = 12;
		String queryAddress = "";
		
		for(int i = 0; i < QDCount; i++)
		{
			queryAddress = getStringFromPoint();
			ptr += 4;
		}
		for(int i = 0; i < ANCount; i++)
		{
			queryAddress = getStringFromPoint();
			
			int queryType = response[ptr++] * 256 + response[ptr++];
			int queryClass = response[ptr++] * 256 + response[ptr++];
			
			ptr += 4; // TTL
			
			int toRead = response[ptr++] * 256 + response[ptr++];
			
			ANList[i] = new Record(queryAddress, queryType, toRead, ptr);
			
			ptr += toRead;
		}
		for(int i = 0; i < NSCount; i++)
		{
			queryAddress = getStringFromPoint();
			
			int queryType = response[ptr++] * 256 + response[ptr++];
			int queryClass = response[ptr++] * 256 + response[ptr++];
			
			ptr += 4; // TTL
			
			int toRead = response[ptr++] * 256 + response[ptr++];
			
			NSList[i] = new Record(queryAddress, queryType, toRead, ptr);
			
			ptr += toRead;
		}
	}

	public String getStringFromPoint()
	{
		String ret = getStringFromPointUncut();
		
		return ret.substring(0, ret.length()-1);
	}

	public String getStringFromPointUncut()
	{
		String ret = "";
		int readLen = response[ptr++];
		
		while(readLen != 0 && readLen != (byte) 11000000)
		{
			for(int c = 0; c < readLen; c++)
			{
				ret += (char) response[ptr++];
			}
			readLen = response[ptr++];
			ret += ".";
		}
		if(readLen == (byte) 11000000)
		{
			int _pos = ptr;
			ptr = response[ptr];
			ret += getStringFromPointUncut();
			ptr = _pos + 1;
		}
		
		return ret;
	}

	public byte[] getDomainBytes(String domain)
	{
		byte[] domainBytes = domain.getBytes();
		byte[] byteArray = new byte[domainBytes.length+2];
		
		for(int i = 0; i < domainBytes.length; i++)
			byteArray[i+1] = domainBytes[i];
		
		int lastDot = 0;
		
		for(int i = 0; i < byteArray.length; i++)
		{
			if(byteArray[i] == '.')
			{
				byteArray[lastDot] = (byte) (i - lastDot - 1);
				lastDot = i;
			}
		}
		byteArray[lastDot] = (byte) (byteArray.length - lastDot - 2);
		
		return byteArray;
	}

	public String getDomain() {
		return domain;
	}

	public void setDNS(String domain) {
		this.domain = domain;
	}

	public int getQType() {
		return QType;
	}

	public void setQType(int qType) {
		QType = qType;
	}
}
