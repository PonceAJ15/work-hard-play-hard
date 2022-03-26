package web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import lombok.SneakyThrows;
import server.W2HPServer;

public class DAppServer extends Thread
{
	@Override
	@SneakyThrows
	public void run()
	{
		Server server = new Server(W2HPServer.getDAppPort());
		ServletContextHandler handler = new ServletContextHandler();
		server.setHandler(handler);
		handler.addServlet(DAppServlet.class, W2HPServer.getDAppURI());
		server.start();
	}
}