package launcher;

import org.web3j.crypto.Hash;

import gui.W2HPTrayIcon;
import mining.JavaMiner;

public class W2HPClientLauncher
{
	public static void main(String[] args)
	{
		byte[] header = {-62, 106, 116, 114, 95, 55, 43, 42, -41, 101, 1, 121, 124, 108, 103,
				         -114, 15, -73, -98, 5, 0, 0, 0, 0, -90, -69, 56, 59, 108, -65, -40, 
				         19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		byte[] target = new byte[32];
		target[0] = -128;
		target[1] = -128;
		target[2] = -128;
		target[3] = -128;
		target[4] = 127;
		JavaMiner miner = new JavaMiner(header, target, 55, b -> {System.out.println("Solution:"+bytesToHex(b));System.exit(0);});
		System.out.println("Starting mining!");
		miner.start();
		System.out.println("Something else!");
	}
	
	public static void hashTests()
	{
		byte[] address = new byte[20];
		for(int i = 0; i < address.length; i++)
			if(i%2 == 0)
				address[i] = (byte)0xFF;
			else
				address[i] = 0x00;
		System.out.println("0x"+bytesToHex(intTo4ByteArray(0)));
		System.out.println("0x"+bytesToHex(testMerkleMiningHash(address,0,0,0)));
		System.out.println(bytesToHex(Hash.sha3(new byte[32])));
	}

	public static byte[] testMerkleMiningHash(byte[] address, int checknum, int location, int nonce)
	{
		byte[] source = new byte[32];
		System.arraycopy(address, 0, source, 0, 20);
		System.arraycopy(intTo4ByteArray(checknum), 0, source, 20, 4);
		System.arraycopy(intTo4ByteArray(location), 0, source, 24, 4);
		System.arraycopy(intTo4ByteArray(nonce), 0, source, 28, 4);
		System.out.println("0x"+bytesToHex(source));
		return Hash.sha3(source);
	}
	
	public static byte[] intTo4ByteArray(int i)
	{
		byte[] sourceCopy = new byte[4];
		sourceCopy[0] = (byte) (i >>> 24);
		sourceCopy[1] = (byte) (i >>> 16);
		sourceCopy[2] = (byte) (i >>> 8);
		sourceCopy[3] = (byte) (i >>> 0);
		return sourceCopy;
	}
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}