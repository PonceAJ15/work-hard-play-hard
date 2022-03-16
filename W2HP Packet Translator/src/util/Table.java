package util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is useless, didn't know about EnumMap before writing.
 * @author Antonio Juan Ponce
 *
 * @param <R> row
 * @param <C> column
 * @param <V> value
 */
public class Table<R, C, V> implements Map<Integer, V>
{
	private int size = 0;
	
	private final HashMap<Integer, V> MAPPINGS = new HashMap<Integer, V>();

	@Override public int size()
		{return size;}
	@Override public boolean isEmpty()
		{return size() == 0;}

	@Override public boolean containsKey(Object key)
		{return MAPPINGS.containsKey(key);}
	public boolean containsCell(R row, C column)
		{return containsKey(row.hashCode()*column.hashCode());}
	@Override public boolean containsValue(Object value)
		{return MAPPINGS.containsValue(value);}
	@Override public V get(Object key) 
		{return MAPPINGS.get(key);}
	public V getCell(R row, C col)
		{return MAPPINGS.get(row.hashCode()*col.hashCode());}

	@Override public V put(Integer key, V value)
		{return MAPPINGS.put(key, value);}
	public V putCell(R row, C col, V value)
		{return MAPPINGS.put(row.hashCode()*col.hashCode(), value);}
	@Override public V remove(Object key)
		{return MAPPINGS.remove(key);}
	public V removeCell(R row, C col)
		{return MAPPINGS.remove(row.hashCode()*col.hashCode());}
	@Override public void putAll(Map<? extends Integer, ? extends V> m)
		{MAPPINGS.putAll(m);}
	public void putAllCells(Table<? extends R, ? extends C, ? extends V> m)
		{MAPPINGS.putAll(m.MAPPINGS);}
	@Override public void clear()
		{MAPPINGS.clear();}
	@Override public Set<Integer> keySet()
		{return MAPPINGS.keySet();}
	@Override public Collection<V> values()
		{return MAPPINGS.values();}
	@Override public Set<Entry<Integer, V>> entrySet()
		{return MAPPINGS.entrySet();}
}