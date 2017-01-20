
import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;

public class Main {
    public static int packetID = 1;

    public static void main(String[] args) {
        //String name = new String(args[0]);
        String domain = new String("www.google.com");

        try {
            connectViaUDP(InetAddress.getByName("8.8.8.8"), domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    static byte[] createQuerry(String domain) {
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

    static void showResult(byte[] resBytes)
    {
        System.out.println(new String (resBytes));
    }

    static void connectViaUDP(InetAddress serverAddr, String domain)
    {
        try {

            byte[] sendData;
            byte[] recievedData = new byte[512];

            sendData = createQuerry(domain);

            DatagramSocket clientSocket;
            clientSocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddr, 53);
            clientSocket.send(sendPacket);
            packetID++;

            DatagramPacket recPacket = new DatagramPacket(recievedData, recievedData.length);
            clientSocket.receive(recPacket);

            if(!checkIfTCSet(recPacket))
            {
                showResult(recPacket.getData());
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
                //dnsFormat += String.valueOf(currentOctets) + temp;
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

