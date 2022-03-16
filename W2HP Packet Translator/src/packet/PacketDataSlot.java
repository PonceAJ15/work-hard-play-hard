package packet;

import java.math.BigDecimal;
import java.math.BigInteger;

import util.Type;

public class PacketDataSlot
{
	private final String NAME;
	private final Type TYPE;
	private final Object VALUE;
	public PacketDataSlot(String name, Type type, Object value)
	{
		this.NAME = name;
		this.TYPE = type;
		this.VALUE = value;
	}
	public byte getByte()
	{
		if(TYPE != Type.BYTE)
			throw new UnsupportedOperationException("Method getByte() invoked on incompatible type "+TYPE.toString().toLowerCase()+".");
		return ((Number)VALUE).byteValue();
	}
	public char getChar()
	{
		if(TYPE != Type.CHAR)
			throw new UnsupportedOperationException("Method getChar() invoked on incompatible type "+TYPE.toString().toLowerCase()+".");
		return ((String)VALUE).charAt(0);
	}
	public int getInt()
	{
		if(TYPE != Type.INT)
			throw new UnsupportedOperationException("Method getInt() invoked on incompatible type "+TYPE.toString().toLowerCase()+".");
		return ((Number)VALUE).intValue();
	}
	public long getLong()
	{
		if(TYPE != Type.LONG)
			throw new UnsupportedOperationException("Method getLong() invoked on incompatible type "+TYPE.toString().toLowerCase()+".");
		return ((Number)VALUE).longValue();
	}
	public byte[] getHash()
	{
		if(TYPE != Type.HASH)
			throw new UnsupportedOperationException("Method getHash() invoked on incompatible type "+TYPE.toString().toLowerCase()+".");
		return new BigInteger(((BigDecimal)VALUE).toPlainString()).toByteArray();
	}
	public String getString()
	{
		if(TYPE != Type.STRING)
			throw new UnsupportedOperationException("Method getString() invoked on incompatible type "+TYPE.toString().toLowerCase()+".");
		return (String)VALUE;
	}
	public Type getType()
		{return TYPE;}
	public String getName()
		{return NAME;}
	public Object getValue()
		{return VALUE;}
}