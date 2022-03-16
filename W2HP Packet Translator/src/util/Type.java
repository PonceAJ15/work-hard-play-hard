package util;

import java.util.HashMap;

public enum Type
{
	BYTE,CHAR,INT,LONG,HASH,BYTE_ARRAY,STRING;

	public static final HashMap<String, Integer> TYPE_SIZE_ASSUMPTIONS = new HashMap<String, Integer>();
	static 
	{
		Type.TYPE_SIZE_ASSUMPTIONS.put("byte",1);
		Type.TYPE_SIZE_ASSUMPTIONS.put("char",1);
		Type.TYPE_SIZE_ASSUMPTIONS.put("int",4);
		Type.TYPE_SIZE_ASSUMPTIONS.put("long", 8);
		Type.TYPE_SIZE_ASSUMPTIONS.put("hash", 32);
		Type.TYPE_SIZE_ASSUMPTIONS.put("byte-array", null);
		Type.TYPE_SIZE_ASSUMPTIONS.put("string", null);
	}
	public String asString(Object o)
	{
		return null; //TODO:
	}
}