import sun.misc.IOUtils;
import sun.nio.ch.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;

public class Main {
    public static int packetID = 1;

    public static void main(String[] args) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName("8.8.8.8");
            byte[] sendData;
            byte[] recievedData = new byte[512];

            //String name = new String(args[0]);
            String name = new String("www.google.com");

            sendData = createQuerry(name);

            DatagramSocket clientSocket;
            clientSocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, 53);
            clientSocket.send(sendPacket);
            packetID++;

            DatagramPacket recPacket = new DatagramPacket(recievedData, recievedData.length);
            clientSocket.receive(recPacket);

            parsePacket(recPacket);

            String answer = new String(recPacket.getData());
            System.out.println(answer);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
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
        //dnsFormat += String.valueOf(currentOctets) + temp + "0";
        buffer.write(currentOctets);

        try {
            buffer.write(temp.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        buffer.write(0x00000000);

        return buffer.toByteArray();
    }

    static void parsePacket(DatagramPacket packet)
    {
        byte[] data = packet.getData();

        System.out.println(data[3]);
        System.out.println(data[4]);
    }

//    //changing 3wwww6google3com0 to www.google.com
//    byte[] fromDNSFormatToDomain(String dnsFormat)
//    {
//        String domain = "";
//        String temp = "";
//        int i = 0;
//        while (i < dnsFormat.length())
//        {
//            int octets = Character.getNumericValue(dnsFormat.charAt(i));
//            int j = 0;
//            for(j = i+1; j <= octets+i; j++)
//            {
//                temp += dnsFormat.charAt(j);
//            }
//
//            domain += temp;
//
//            if(j < dnsFormat.length() - 1)
//                domain += '.';
//
//            temp = "";
//            i = j;
//        }
//
//        return domain;
//    }
}

