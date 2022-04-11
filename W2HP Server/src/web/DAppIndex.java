package web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.W2HPServer;

//this servlet is for embedded use only and won't be serialized.
@SuppressWarnings("serial")
public final class DAppIndex extends HttpServlet
{
	private static final byte[] BYTES;
	static {
		if(W2HPServer.isArgumentsLoaded())
			try
				{BYTES = Pages.read(W2HPServer.getDAppPath());}
			catch (IOException e)
				{throw new ExceptionInInitializerError(e);}
		else
			throw new ExceptionInInitializerError(new IllegalStateException(
					"Static loading of index page bytes failed due to class"+
					DAppIndex.class.getSimpleName()+
					" being accessed before "+
					W2HPServer.class.getSimpleName()+
					" could load arguments and configuration settings."));
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
		{Pages.serve(req, resp, BYTES);}
}