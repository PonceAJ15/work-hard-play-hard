package web;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Pages
{
	private Pages() 
		{throw new UnsupportedOperationException(Pages.class.getName()+" is not instantiatable.");}
	
	public static byte[] read(String path) throws IOException 
	{
		return Files.readAllBytes(Paths.get(path));
	}
	
	public static void serve(HttpServletRequest req, HttpServletResponse resp, byte[] bytes) throws IOException
	{
		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		
		OutputStream os = resp.getOutputStream();
		os.write(bytes);
		os.flush();
	}
}