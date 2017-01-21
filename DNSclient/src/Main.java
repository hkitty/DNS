import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Main {
    public static int packetID = 1;
    public enum TYPE {NONE, A, NS, MD, MF, CNAME, SOA, MB, MG, MR, NULL, WKS, PTR, HINFO, MINFO, MX, TXT, AAAA}
    public enum CLASS {NONE, IN, CS, CH, HS}

    public static void main(String[] args) {
        //String name = new String(args[0]);
        String domain = new String("www.google.com");

        try {
            connectViaUDP(InetAddress.getByName("8.8.8.8"), domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    static byte[] createQuery(String domain) {
        byte[] dnsFormatDomain = changeToDNSNameFormat(domain);
        byte[] query;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            byte i;
            i = (byte) packetID;
            buffer.write(new byte[]{0b0, i}); //id
            buffer.write(new byte[]{0b00000001, 0b00000000});//Flags
            buffer.write(new byte[]{0b0, 0b1}); //QDCount
            buffer.write(new byte[]{0b0, 0b0}); //ANCount
            buffer.write(new byte[]{0b0, 0b0}); //NSCount
            buffer.write(new byte[]{0b0, 0b0}); //ARCount

            //creating question
            buffer.write(dnsFormatDomain); //QName
            buffer.write(0b00000000);//QType first octet
            buffer.write(0xFF);//QType second octet
            buffer.write(new byte[]{0b00000000, 0b00000001}); //QClass

        } catch (IOException e) {
            e.printStackTrace();
        }

        query = buffer.toByteArray();
        return query;
    }

    static boolean checkIfTCSet(DatagramPacket packet)
    {
        byte[] data = packet.getData();
        byte flags = data[2];
        if(((flags >> 1) & 1) == 0)
            return false;
        else return true;
    }

    static void showResult(byte[] resBytes, String domain)
    {

        int numOfRecords = resBytes[7];
        int answerBegins = createQuery(domain).length;

        int i = answerBegins;
        for(int iterator = 0; iterator < numOfRecords; iterator++)
        {
            int type = resBytes[i + 3];
            TYPE eType;
            if(type == 28)
                eType = TYPE.AAAA;
            else eType = TYPE.values()[type];
            System.out.print("\nType: " + eType.name());

            int dnsClass = resBytes[i + 5];
            CLASS eClass = CLASS.values()[dnsClass];
            System.out.print(" Class: " + eClass.name());

            byte[] ttlBytes = { resBytes[i+6], resBytes[i+7], resBytes[i+8], resBytes[i+9] };
            int ttl = ByteBuffer.wrap(ttlBytes).getInt();
            System.out.print(" TTL: " + ttl);

            int iplength = resBytes[i+11];
            System.out.print(" IP Addres: ");

            if(eType != TYPE.AAAA) {
                int n;
                for (n = 0; n < iplength - 1; n++) {
                    int temp = (resBytes[i + 12 + n] & 0b11111111);
                    System.out.print(temp + ".");
                }
                int temp = (resBytes[i + 12 + n] & 0b11111111);
                System.out.print(temp);
            }
            else
            {
                int n;
                for (n = 0; n < iplength; n+=2) {
                    for (int j = 0; j < 2; j++)
                    {
                        byte[] word = {resBytes[i + 12 + n + j]};
                        StringBuilder builder = new StringBuilder();
                        for(byte b : word) {
                            builder.append(String.format("%02x", b));
                        }
                        System.out.print(builder.toString());
                    }
                    if(n != iplength - 2)
                        System.out.print(":");
                }
            }

            i += 12 + iplength;
        }
    }

    static void connectViaUDP(InetAddress serverAddr, String domain)
    {
        try {

            byte[] sendData;
            byte[] receivedData = new byte[512];

            sendData = createQuery(domain);

            DatagramSocket clientSocket;
            clientSocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddr, 53);
            clientSocket.send(sendPacket);
            packetID++;

            DatagramPacket recPacket = new DatagramPacket(receivedData, receivedData.length);
            clientSocket.receive(recPacket);

            if(!checkIfTCSet(recPacket))
            {
                showResult(recPacket.getData(), domain);
            }
            else
            {
                connectViaTCP(serverAddr, domain);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void connectViaTCP(InetAddress serverAddr, String domain)
    {
        try {
            byte[] query = createQuery(domain);
            Socket s = new Socket(serverAddr, 53);
            short i = (short) query.length;

            DataOutputStream inputBuff = new DataOutputStream(s.getOutputStream());
            DataInputStream outBuff = new DataInputStream(s.getInputStream());

            inputBuff.writeShort(i);
            inputBuff.write(query);

            short len;
            len = outBuff.readShort();
            byte buf[] = new byte[len];
            outBuff.read(buf);
            showResult(buf, domain);

            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //changing www.google.com to 3wwww6google3com0
    static byte[] changeToDNSNameFormat(String domain)
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int i = 0;
        int currentOctets = 0;
        String temp = "";
        while (i < domain.length())
        {
            if(domain.charAt(i) != '.') {
                currentOctets++;
                temp = temp + domain.charAt(i);
            }
            else
            {
                buffer.write(currentOctets);

                try {
                    buffer.write(temp.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                currentOctets = 0;
                temp = "";
            }
            i++;
        }

        buffer.write(currentOctets);

        try {
            buffer.write(temp.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        buffer.write(0x00000000);

        return buffer.toByteArray();
    }
}

