package util;

import java.util.HashMap;

/**
 * This class is useless, didn't know about EnumMap before writing.
 * @author Antonio Juan Ponce
 *
 * @param <K> key
 * @param <V> value
 */
public class HashContext<K, V>
{
	public class Context
	{
		private final K KEY;
		private final HashMap<K, V> ENTRIES = new HashMap<K, V>();
		private Context(K key)
		{
			this.KEY = key;
		}
		public Context put(K key, V value)
		{
			MAPPINGS.put(key, value);
			ENTRIES.put(key, value);
			return this;
		}
		public V get(K key)
		{
			return ENTRIES.get(key);
		}
		public K key()
		{
			return KEY;
		}
	}
	
	private HashMap<K, V> MAPPINGS = new HashMap<K, V>();
	private HashMap<K, Context> CONTEXT_MAPPINGS = new HashMap<K, Context>();
	
	public Context put(K key, V value)
	{
		if(CONTEXT_MAPPINGS.get(key) == null)
		{
			Context ret = new Context(key);
			CONTEXT_MAPPINGS.put(key, ret);
			MAPPINGS.put(key, value);
			return ret;
		}
		return CONTEXT_MAPPINGS.get(key);
	}
	public V get(K key)
	{
		return MAPPINGS.get(key);
	}
	
	
	public Context getContext(K key)
	{
		return CONTEXT_MAPPINGS.get(key);
	}
	
}