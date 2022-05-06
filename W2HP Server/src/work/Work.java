package work;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.web3j.crypto.Hash;

import lombok.Getter;

public class Work<Client> implements Serializable
{
	private final class WriteThread extends Thread
	{
		private final byte[] nonce = new byte[32];
		private final long currentJob;
		private WriteThread(byte[] nonce, long currentJob)
		{
			if(nonce.length != 32) throw new IllegalArgumentException("Expected 32 byte nonce.");
			System.arraycopy(nonce, 0, this.nonce, 0, 32);
			this.currentJob = currentJob;
		}
		
		@Override public void run()
		{
			synchronized(secretPath)
			{
				try(RandomAccessFile RAF = new RandomAccessFile(new File(secretPath), "rw"))
				{
					RAF.seek(NONCE_OFFSET);
					RAF.write(nonce);
					RAF.seek(JOB_OFFSET);
					RAF.writeLong(currentJob);
				} catch (IOException e) {
					//This should be made known to the server owner.
					e.printStackTrace();
				}
			}
		}
	}
	
	private static final long serialVersionUID = 1L;
	private static final int FILE_HEADER_LENGTH = 108;
	private static final int SEED_OFFSET = 100;
	private static final int NONCE_OFFSET = 36;
	private static final int JOB_OFFSET = 68;
	private static final BigInteger MAX_HASH = BigInteger.TWO.pow(256).subtract(BigInteger.ONE);
	
	private Map<Client, Long> jobs = new HashMap<Client, Long>();
	private Map<Client, Semaphore> works = new HashMap<Client, Semaphore>();

	private long currentJob;
	@Getter private final byte[] target = new byte[32];
	private final byte[] address = new byte[20];
	private final byte[] checkNum = new byte[4];
	private final byte[] publicSeed = new byte[8];
	private final byte[] secretSeed = new byte[8];
	private final byte[] bestNonce = new byte[32];
	@Getter private final byte[] publicHeader = new byte[64];
	@Getter private final byte[] secretHeader = new byte[64];
	@Getter private BigInteger bestValue = BigInteger.ZERO;
	@Getter private byte[] currentSecret = new byte[32];
	@Getter private byte[] currentSolution = new byte[32];
	private int index;
	private String secretPath;
	public Work(int entries, byte[] address, byte[] checkNum, byte[] seed, long targetValue, String secretPath) throws FileNotFoundException, IOException
	{
		if(address.length != 20) throw new IllegalArgumentException("Expected 20 byte address.");
		if(checkNum.length != 4) throw new IllegalArgumentException("Expected 4 byte checknum.");
		if(seed.length != 8) throw new IllegalArgumentException("Expected 8 byte seed.");
		BigInteger temp = MAX_HASH.divide(BigInteger.valueOf(targetValue));
		byte[] tempTarget = temp.toByteArray();
		if(tempTarget.length <= 32)
			System.arraycopy(tempTarget, 0, target, 0, tempTarget.length);
		else
			System.arraycopy(tempTarget, 0, target, 0, 32);
		this.currentJob = Long.MIN_VALUE;
		byte[] secrets = new byte[entries * 32];
		byte[] hash = new byte[32];
		
		try (RandomAccessFile RAF = new RandomAccessFile(new File(secretPath), "rw"))
		{
			index = entries-1;
			System.arraycopy(address, 0, this.address, 0, 20);
			System.arraycopy(checkNum, 0, this.checkNum, 0, 4);
			System.arraycopy(seed, 0, this.publicSeed, 0, 8);
			RAF.seek(0);
			RAF.writeInt(index); //0 -> 4
			RAF.write(target);   //4 -> 36
			RAF.write(bestNonce);//36 -> 68
			RAF.writeLong(this.currentJob); //68 -> 76
			RAF.write(address); //76 -> 96
			RAF.write(checkNum);//96 -> 100
			RAF.write(seed);
			try {
				SecureRandom.getInstanceStrong().nextBytes(hash);
			} catch (NoSuchAlgorithmException e) {
				throw new SecurityException("Can't generate initial bytes securely.");
			}
			System.arraycopy(hash, 0, secrets, 0, 32);
			for(int i = 32; i < entries*32; i+=32)
			{
				System.arraycopy(secrets, i-32, hash, 0, 32);
				hash = Hash.sha3(hash);
				System.arraycopy(hash, 0, secrets, i, 32);
			}
			RAF.write(secrets);
			this.secretPath = secretPath;
			System.arraycopy(secrets, (entries-1)*32, this.currentSecret, 0, 32);
			System.arraycopy(secrets, (entries-2)*32, this.currentSolution, 0, 32);
			for(int i = 0; i < this.secretSeed.length; i++)
				this.secretSeed[i] = (byte)(this.publicSeed[i] ^ this.currentSolution[24+i]);
			setPublicHeader();
			setSecretHeader();
		}
	}
	public Work(String filepath) throws IOException
	{
		this.currentSolution = new byte[32];
		this.currentSecret = new byte[32];
		this.secretPath = filepath;
		try(RandomAccessFile RAF = new RandomAccessFile(new File(filepath), "rw"))
		{
			RAF.seek(0);
			this.index = RAF.readInt();
			RAF.read(this.target);
			RAF.read(this.bestNonce);
			this.currentJob = RAF.readLong();
			RAF.read(this.address);
			RAF.read(this.checkNum);
			RAF.read(this.publicSeed);
			RAF.seek(((index-1)*32)+FILE_HEADER_LENGTH);
			RAF.read(currentSolution);
			RAF.read(currentSecret);
			setPublicHeader();
			setSecretHeader();
			bestValue = calculateValue(this.bestNonce);
		}
	}
	public void setSeed(byte[] newSeed) throws IOException
	{
		synchronized(secretPath)
		{
			try(RandomAccessFile RAF = new RandomAccessFile(new File(secretPath), "rw"))
			{
				if(newSeed.length != 8) throw new IllegalArgumentException("Expected 8 byte seed.");
				System.arraycopy(newSeed, 0, this.publicSeed, 0, 8);
				RAF.seek(SEED_OFFSET);
				RAF.write(newSeed);
				setSecretHeader();
				setPublicHeader();
			}
		}
	}
	public void next(byte[] newSeed) throws IOException
	{
		synchronized(secretPath)
		{
			try(RandomAccessFile RAF = new RandomAccessFile(new File(secretPath), "rw"))
			{
				if(newSeed.length != 8) throw new IllegalArgumentException("Expected 8 byte seed.");
				System.arraycopy(newSeed, 0, this.publicSeed, 0, 8);
				this.index--;
				RAF.seek(0);
				RAF.writeInt(index);
				RAF.seek(SEED_OFFSET);
				RAF.write(newSeed);
				RAF.seek(JOB_OFFSET);
				RAF.writeLong(currentJob);
				RAF.seek(((index-1)*32)+FILE_HEADER_LENGTH);
				RAF.read(currentSolution);
				RAF.read(currentSecret);
				int tmpNum = (checkNum[0] << 24) | (checkNum[1] << 16)+(checkNum[2] << 8)+checkNum[3];
				tmpNum ++;
				checkNum[0] = (byte)(tmpNum >>> 24);
				checkNum[1] = (byte)(tmpNum >>> 16);
				checkNum[2] = (byte)(tmpNum >>> 8);
				checkNum[3] = (byte)(tmpNum >>> 0);
			}
		}
	}
	
	private void setSecretHeader()
	{
		System.arraycopy(this.address, 0, this.secretHeader, 0, 20);
		System.arraycopy(this.checkNum, 0, this.secretHeader, 20, 4);
		System.arraycopy(this.secretSeed, 0, this.secretHeader, 24, 8);
	}
	private void setPublicHeader()
	{
		System.arraycopy(this.address, 0, this.publicHeader, 0, 20);
		System.arraycopy(this.checkNum, 0, this.publicHeader, 20, 4);
		System.arraycopy(this.publicSeed, 0, this.publicHeader, 24, 8);
	}
	
	public boolean checkSolution(Client client, byte[] nonce)
	{
		if(nonce.length != 32)
			throw new IllegalArgumentException("Nonce size of 32 bytes expected.");
		boolean success = true;
		if(jobs.get(client) == null)
			success = false;
		else
		{
			long job = jobs.get(client);
			long claimed = (nonce[0] << 56) |
					(nonce[1] << 48) +
					(nonce[2] << 40) +
					(nonce[3] << 32) +
					(nonce[4] << 24) +
					(nonce[5] << 16) +
					(nonce[6] << 8)  +
					(nonce[7] << 0);
			if(job != claimed)
				success = false;
		}
		System.arraycopy(nonce,0,publicHeader,32,32);
		byte[] hash = Hash.sha3(publicHeader);
		for(int i = 0; i < target.length; i++)
			if(target[i] < hash[i])
			{
				success = false;
				break;
			}else if(hash[i] < target[i])
				break;
		System.arraycopy(hash, 0, secretHeader, 32, 32);
		byte[] secret = Hash.sha3(secretHeader);
		//Initial value calculation
		BigInteger value = MAX_HASH.divide(new BigInteger(1, hash));
		//Calculation of true value
		value = value.multiply(MAX_HASH.divide(new BigInteger(1,secret)));
		//Check is the hash is better than the previous hash.
		if(value.compareTo(bestValue) > 0)
		{
			//somehow they added value to the system, they deserve success
			success = true;
			this.bestValue = value;
			System.arraycopy(nonce, 0, bestNonce, 0, 32);
			//launch a new thread to prevent writing from blocking this one.
			new WriteThread(nonce, currentJob).start();
		}
		
		if(success)
		{
			if(works.get(client) == null)
				works.put(client, new Semaphore(0));
			works.get(client).release();
		}
		
		return success;
	}
	private BigInteger calculateValue(byte[] nonce)
	{
		System.arraycopy(nonce, 0, publicHeader, 32, 32);
		byte[] hash = Hash.sha3(publicHeader);
		System.arraycopy(nonce, 0, secretHeader, 32, 32);
		byte[] secret = Hash.sha3(secretHeader);
		BigInteger ret = MAX_HASH.divide(new BigInteger(1, hash));
		ret = ret.multiply(MAX_HASH.divide(new BigInteger(1, secret)));
		return ret;
	}
	
	public void aquireWork(Client client, int jobs) throws InterruptedException
	{
		if(works.get(client) == null)
			works.put(client, new Semaphore(0));
		works.get(client).acquire(jobs);
	}
	
	public byte[] getHeader(Client client) throws NullPointerException
	{
		byte[] ret = new byte[64];
		long job = jobs.get(client);
		System.arraycopy(publicHeader, 0, ret, 0, 64);
		ret[32] = (byte)(job >>> 56);
		ret[33] = (byte)(job >>> 48);
		ret[34] = (byte)(job >>> 40);
		ret[35] = (byte)(job >>> 32);
		ret[36] = (byte)(job >>> 24);
		ret[37] = (byte)(job >>> 16);
		ret[38] = (byte)(job >>> 8);
		ret[39] = (byte)(job >>> 0);
		return ret;
	}
	
	public void giveJob(Client client)
	{
		jobs.put(client, currentJob++);
	}
	public void forgetJob(Client client)
	{
		jobs.put(client, null);
	}
	public void requestJob(Client client)
	{
		if(jobs.get(client) == null)
			jobs.put(client, currentJob++);
	}
}