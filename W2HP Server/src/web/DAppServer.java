package web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import server.W2HPServer;

public class DAppServer
{
	public void start() throws Exception
	{
		Server server = new Server(W2HPServer.getDAppPort());
		ServletContextHandler handler = new ServletContextHandler();
		server.setHandler(handler);
		handler.addServlet(DAppIndex.class, W2HPServer.getDAppURI());
		server.start();
	}
}