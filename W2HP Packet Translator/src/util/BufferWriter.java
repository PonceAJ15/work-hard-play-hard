package util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BufferWriter
{
	private int index = 0;
	private final byte[] BUFFER;
	public BufferWriter(byte[] buffer)
	{
		this.BUFFER = buffer;
	}
	
	public void writeType(Type t, Object o)
	{
		switch(t)
		{
			case BYTE:
				writeByte((Byte) o);
				break;
			case CHAR:
				writeChar((Character) o);
				break;
			case INT:
				writeInt((Integer) o);
				break;
			case LONG:
				writeLong((Long) o);
				break;
			case HASH:
				writeHash((byte[]) o);
				break;
			case BYTE_ARRAY:
				writeByteArray((byte[]) o);
				break;
			case STRING:
				writeString((String) o);
				break;
			default:
				throw new IllegalStateException("Unrecognized type constant, value should not exist.");
		}
	}
	
	public void writeByte(byte b)
		{BUFFER[index++] = b;}
	public void writeChar(char c)
		{BUFFER[index++] = (byte) c;}
	public void writeInt(int i)
		{System.arraycopy(ByteBuffer.allocate(4).putInt(i).array(), 0, BUFFER, index, 4);index+=4;}
	public void writeLong(long l)
		{System.arraycopy(ByteBuffer.allocate(8).putLong(l).array(), 0, BUFFER, index, 8);index+=8;}
	public void writeHash(byte[] h)
		{System.arraycopy(h, 0, BUFFER, index, 32);index+=32;}
	public void writeByteArray(byte[] b)
		{BUFFER[index++] = (byte) b.length;System.arraycopy(b, 0, BUFFER, index, b.length);index+=b.length;}
	public void writeString(String s)
		{BUFFER[index++] = (byte) s.length();System.arraycopy(s.getBytes(StandardCharsets.UTF_8), 0, BUFFER, index, s.length());index+=s.length();}
}