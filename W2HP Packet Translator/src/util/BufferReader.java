package util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class BufferReader
{
	private int index = 0;
	private final byte[] BUFFER;
	public BufferReader(byte[] buffer)
	{
		this.BUFFER = buffer;
	}
	
	public Object readValue(Type t)
	{
		switch(t)
		{
			case BYTE:
				return readByte();
			case CHAR:
				return readChar();
			case INT:
				return readInt();
			case LONG:
				return readLong();
			case HASH:
				return readHash();
			case BYTE_ARRAY:
				return readByteArray();
			case STRING:
				return readString();
			default:
				throw new IllegalStateException("Unrecognized type constant, value should not exist.");
		}
	}
	
	public Byte readByte()
		{return BUFFER[index++];}	
	public Character readChar()
		{return (char) BUFFER[index++];}
	public Integer readInt()
	{
		byte[] temp = new byte[4];
		System.arraycopy(BUFFER, index, temp, 0, 4);
		index += 4;
		return ByteBuffer.wrap(temp).getInt();
	}	
	public Long readLong()
	{
		byte[] temp = new byte[8];
		System.arraycopy(BUFFER, index, temp, 0, 8);
		index += 8;
		return ByteBuffer.wrap(temp).getLong();
	}
	public byte[] readHash()
	{
		byte[] ret = new byte[32];
		System.arraycopy(BUFFER, index, ret, 0, 32);
		index+=32;
		return ret;
	}
	public byte[] readByteArray()
	{
		byte length = BUFFER[index++];
		byte[] ret = new byte[length];
		System.arraycopy(BUFFER, index, ret, 0, length);
		index += length;
		return ret;
	}
	public String readString()
	{
		byte length = BUFFER[index++];
		byte[] temp = new byte[length];
		System.arraycopy(BUFFER, index, temp, 0, length);
		index += length;
		return new String(temp, StandardCharsets.UTF_8);
	}
	
}