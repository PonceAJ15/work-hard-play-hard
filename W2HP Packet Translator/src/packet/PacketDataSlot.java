package packet;

import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.Data;
import util.Type;

@Data
public class PacketDataSlot
{
	private final String name;
	private final Type type;
	private final Object value;
	
	public byte getByte()
	{
		if(type != Type.BYTE)
			throw new UnsupportedOperationException("Method getByte() invoked on incompatible type "+type.toString().toLowerCase()+".");
		return ((Number)value).byteValue();
	}
	public char getChar()
	{
		if(type != Type.CHAR)
			throw new UnsupportedOperationException("Method getChar() invoked on incompatible type "+type.toString().toLowerCase()+".");
		return ((String)value).charAt(0);
	}
	public int getInt()
	{
		if(type != Type.INT)
			throw new UnsupportedOperationException("Method getInt() invoked on incompatible type "+type.toString().toLowerCase()+".");
		return ((Number)value).intValue();
	}
	public long getLong()
	{
		if(type != Type.LONG)
			throw new UnsupportedOperationException("Method getLong() invoked on incompatible type "+type.toString().toLowerCase()+".");
		return ((Number)value).longValue();
	}
	public byte[] getHash()
	{
		if(type != Type.HASH)
			throw new UnsupportedOperationException("Method getHash() invoked on incompatible type "+type.toString().toLowerCase()+".");
		return new BigInteger(((BigDecimal)value).toPlainString()).toByteArray();
	}
	public String getString()
	{
		if(type != Type.STRING)
			throw new UnsupportedOperationException("Method getString() invoked on incompatible type "+type.toString().toLowerCase()+".");
		return (String)value;
	}
}