package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Uses daisy chaining for tree traversal.
 * Expects object user to know the organization of data in JSON file.
 * @author Antonio Juan Ponce
 */
public class JSONObject
{
	//TODO: implement boolean data type
	public enum JSONFieldType
	{
		OBJECT, LIST, NUMBER, STRING, COLLECTION, VALUE;
	}
	public abstract class JSONField  implements Iterable<JSONField>
	{
		//evil java be like: type safety is for cowards
		private UnsupportedOperationException e(JSONFieldType type)  
		{
			return new UnsupportedOperationException(
				"Method meant for JSON field of type "+
					type.toString().toLowerCase()+
				" invoked on JSON field of type "+
					TYPE.toString().toLowerCase()+
				"."
			);
		}
		
		public final JSONFieldType TYPE;
		private JSONField(JSONFieldType type)
		{
			this.TYPE = type;
		}
		
		//get entry set from an object
		public Set<Entry<String,JSONField>> getFields()
			{throw e(JSONFieldType.OBJECT);}			
		
		//get a field from an object
		public JSONField getField(String field)
			{throw e(JSONFieldType.OBJECT);}
		
		//iterate through list
		public Iterator<JSONField> iterator()
				{throw e(JSONFieldType.COLLECTION);}
		
		//get an object from a list
		public JSONField getObjectByFieldNumber(String field, Number value)
			{throw e(JSONFieldType.LIST);}
		public JSONField getObjectByFieldString(String field, String value)
			{throw e(JSONFieldType.LIST);}
		
		//get the value in a object.
		public BigDecimal number()
			{throw e(JSONFieldType.NUMBER);}
		public String string()
			{throw e(JSONFieldType.STRING);}
		public abstract Object getObject();
	}
	private class JSONObjectField extends JSONField
	{
		private final HashMap<String, JSONField> FIELDS = new HashMap<String, JSONField>();
		private JSONObjectField()
			{super(JSONFieldType.OBJECT);}
		
		@Override public Object getObject()
			{return FIELDS;}
		@Override public Set<Entry<String,JSONField>> getFields()
			{return FIELDS.entrySet();}
		@Override public JSONField getField(String field)
			{return FIELDS.get(field);}
		@Override public Iterator<JSONField> iterator()
		{
			ArrayList<JSONField> list = new ArrayList<JSONField>(FIELDS.size());
			for(Entry<String,JSONField> e:FIELDS.entrySet())
				list.add(e.getValue());
			return list.iterator();
		}
	}
	private class JSONListField extends JSONField
	{
		private final ArrayList<JSONField> ELEMENTS = new ArrayList<JSONField>();
		private JSONListField()
			{super(JSONFieldType.LIST);}
		@Override public Object getObject()
			{return ELEMENTS;}
		@Override public Iterator<JSONField> iterator()
			{return ELEMENTS.iterator();}
		//If statement to check type safety, many such cases!
		@Override public JSONField getObjectByFieldNumber(String field, Number value)
		{
			for(JSONField object: this)
				if(object.TYPE == JSONFieldType.OBJECT)
					if(object.getField(field) != null)
						if(object.getField(field).TYPE == JSONFieldType.NUMBER)
							if(object.getField(field).number().equals(value))
								return object;
			return null;
		}
		@Override public JSONField getObjectByFieldString(String field, String value)
		{
			for(JSONField object: this)
				if(object.TYPE == JSONFieldType.OBJECT)
					if(object.getField(field) != null)
						if(object.getField(field).TYPE == JSONFieldType.STRING)
							if(object.getField(field).string().equals(value))
								return object;
							else
								System.out.println(object.getField(field).string() + " != "+value);
			return null;
		}
	}
	private class JSONNumberField extends JSONField
	{
		private final BigDecimal NUMBER;
		private JSONNumberField(BigDecimal number)
		{
			super(JSONFieldType.NUMBER);
			this.NUMBER = number;
		}
		
		@Override public Object getObject()
			{return NUMBER;}
		@Override public BigDecimal number()
			{return NUMBER;}
	}
	private class JSONStringField extends JSONField
	{
		private final String STRING;
		private JSONStringField(String string)
		{
			super(JSONFieldType.STRING);
			this.STRING = string;
		}
		
		@Override public Object getObject()
			{return STRING;}
		@Override public String string()
			{return STRING;}
		@Override public String toString()
			{return STRING;}
	}

	//TODO: implement proper comma parsing
	//TODO: fix bug where strings containing commas are split by scanner
	private static final Pattern DELIMITER      = Pattern.compile("[\\s\\t\\r\\n,]+");
	private static final Pattern OBJECT_START   = Pattern.compile("\\{");
	private static final Pattern OBJECT_END     = Pattern.compile("}"); 
	//https://stackoverflow.com/questions/6525556/regular-expression-to-match-escaped-characters-quotes
	private static final Pattern FIELD          = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\":");
	private static final Pattern STRING_CONTENTS = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
	private static final Pattern STRING         = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"");
	private static final Pattern LIST_START     = Pattern.compile("\\[");
	private static final Pattern LIST_END       = Pattern.compile("]");
	private static final Pattern NULL           = Pattern.compile("null");
	
	//root of initial JSONField object
	private JSONObjectField root;
	
	public JSONObject(String filepath) throws FileNotFoundException, ParseException
	{
		File file = new File(filepath);
		Scanner sc = new Scanner(file);
		sc.useDelimiter(DELIMITER);
		
		//root level is implicit object
		root = new JSONObjectField();
		try
		{				
			while(sc.hasNext(FIELD))
				root.FIELDS.put(parseField(sc), parseValue(sc));
		}catch(ParseException e) {
			throw e;
		}finally {
			sc.close();
		}
	}
	
	private String parseField(Scanner sc) throws ParseException
	{
		Matcher m = STRING_CONTENTS.matcher(sc.next(FIELD));
		if(m.find())
			return m.group(1);
		else
			throw new ParseException("Field name expected but not found",0);		
	}
	private JSONField parseValue(Scanner sc) throws ParseException
	{
		if(sc.hasNext(OBJECT_START))
			return parseObject(sc);
		else if(sc.hasNext(LIST_START))
			return parseList(sc);
		else if(sc.hasNext(STRING))
			return parseString(sc);
		else if(sc.hasNext(NULL))
			return parseNull(sc);
		else if(sc.hasNextBigDecimal())
			return parseNumber(sc);
		else
		{
			System.out.println(sc.nextLine());
			throw new ParseException("Object, List, String, Number or null expected as value", 0);
		}
	}
	private boolean hasNextValue(Scanner sc)
	{
		return sc.hasNext(OBJECT_START) || sc.hasNext(LIST_START) || sc.hasNext(STRING) || sc.hasNext(NULL) || sc.hasNextBigDecimal();
	}
	
	private JSONField parseObject(Scanner sc) throws ParseException
	{
		sc.next(OBJECT_START);
		JSONObjectField ret = new JSONObjectField();
		
		while(sc.hasNext(FIELD))
			ret.FIELDS.put(parseField(sc), parseValue(sc));
		
		if(!sc.hasNext(OBJECT_END))
		{
			for(Entry<String, JSONField> e:ret.getFields())
				System.out.println(e.getKey()+e.getValue());
			throw new ParseException("Closing bracket or comma expected in object", 0);
		}
		
		sc.next(OBJECT_END);
		return ret;
	}
	private JSONField parseList(Scanner sc) throws ParseException
	{
		sc.next(LIST_START);
		JSONListField ret = new JSONListField();
		
		while(hasNextValue(sc))
			ret.ELEMENTS.add(parseValue(sc));
		
		if(!sc.hasNext(LIST_END))
			throw new ParseException("Closing square bracket or comma expected in list", 0);
		
		sc.next(LIST_END);
		return ret;
	}
	private JSONField parseString(Scanner sc)
	{
		Matcher m = STRING_CONTENTS.matcher(sc.next(STRING));
		m.find();
		return new JSONStringField(m.group(1));
	}
	private JSONField parseNumber(Scanner sc)
	{
		return new JSONNumberField(sc.nextBigDecimal());
	}
	private JSONField parseNull(Scanner sc)
	{
		sc.next(NULL);
		return null;
	}
	
	public JSONField getField(String field)
	{
		return root.getField(field);
	}
}