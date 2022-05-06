package mining;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.web3j.crypto.Hash;

public class JavaMiner extends Miner
{
	private AtomicBoolean done = new AtomicBoolean(false);
	private class WorkThread extends Thread
	{	
		private long nonceStart;
		private WorkThread(long nonceStart)
			{this.nonceStart = nonceStart;}
		
		@Override public void run()
		{
			byte[] sourceCopy = new byte[source.length];
			byte[] hash;
			long nonce = nonceStart;
			System.arraycopy(source, 0, sourceCopy, 0, source.length);
			mineCrypto:
			for(;;)
			{
				nonce++;
				//insert value
				sourceCopy[nonceIndex] = (byte) (nonce >>> 56);
				sourceCopy[nonceIndex+1] = (byte) (nonce >>> 48);
				sourceCopy[nonceIndex+2] = (byte) (nonce >>> 40);
				sourceCopy[nonceIndex+3] = (byte) (nonce >>> 32);
				sourceCopy[nonceIndex+4] = (byte) (nonce >>> 24);
				sourceCopy[nonceIndex+5] = (byte) (nonce >>> 16);
				sourceCopy[nonceIndex+6] = (byte) (nonce >>> 8);
				sourceCopy[nonceIndex+7] = (byte) (nonce >>> 0);
				//perform hash
				hash = Hash.sha3(sourceCopy);
				
				if(nonce%128000 == 0)
					System.out.println(bytesToHex(hash));
				
				//check if hash is less than or equal to target
				for(int i = 0; i < target.length; i++)
					if(target[i] < hash[i])
						{continue mineCrypto;}
					else if(hash[i] < target[i])
						break;
				break;
			}
			submitSolution(nonce);
		}
	}
	
	private WorkThread[] workers;
	
	public JavaMiner(byte[] source, byte[] target, int nonceIndex, Consumer<byte[]> ret)
		{super(source, target, nonceIndex, ret);}
	
	//2^32 is close enough
	private final long MAX_UNSIGNED_INT = 4294967296l;
	@Override public void start()
	{
		new WorkThread(0).start();
	}
	
	private synchronized void submitSolution(long nonce)
	{
		ret.accept(new byte[] {(byte)(nonce >>> 56),(byte)(nonce >>> 48),(byte)(nonce >>> 40),(byte)(nonce >>> 32),(byte)(nonce >>> 24),(byte)(nonce >>> 16),(byte)(nonce >>> 8),(byte)(nonce >>> 0)});
		done.set(true);
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