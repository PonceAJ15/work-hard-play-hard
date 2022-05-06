package web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import server.W2HPServer;
import work.WorkServer;

public class DAppServer
{
	public void start() throws Exception
	{
		Server internalServer = new Server(W2HPServer.getDAppPort());
		ServletContextHandler internalHandler = new ServletContextHandler();
		internalServer.setHandler(internalHandler);
		internalHandler.addServlet(DAppIndex.class, W2HPServer.getDAppURI());
		internalServer.start();
		
		Server externalServer = new Server(W2HPServer.getServicePort());
		ServletContextHandler externalHandler = new ServletContextHandler();
		externalServer.setHandler(externalHandler);
		externalHandler.addServlet(WorkServer.class, W2HPServer.getServiceURI());
		externalServer.start();
	}
}