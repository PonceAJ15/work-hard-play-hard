package util;

public interface FieldLabelList<E extends Enum<E> & FieldLabel> extends FieldLabel
{
	public Enum<E>[] getElements();
	public Class<E> getFieldClass();
}