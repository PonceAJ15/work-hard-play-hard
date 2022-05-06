package mining;

import java.util.function.Consumer;

public abstract class Miner
{
	protected byte[]         source;
	protected int        nonceIndex;
	protected byte[]         target;
	protected Consumer<byte[]>  ret;
	
	public Miner(byte[] source, byte[] target, int nonceIndex, Consumer<byte[]> ret)
	{
		this.source = source;
		this.target = target;
		this.nonceIndex = nonceIndex;
		this.ret = ret;
	}
	
	public abstract void start();
}