package web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import server.W2HPServer;

//this class won't be serialized.
@SuppressWarnings("serial")
public final class DAppServlet extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		//HTTP protocol: let client know that the server is responding to GET request with an HTML file 
		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
				
		//send HTML file to client
		OutputStream os = resp.getOutputStream();
		@Cleanup BufferedReader br = new BufferedReader(new FileReader(W2HPServer.getDAppPath()));
		String currLine;
		while((currLine = br.readLine()) != null)
			os.write(currLine.getBytes(Charset.forName("UTF-8")));
		os.flush();
	}
}