package gui;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class W2HPTrayIcon
{

	public static void launchGUI()
	{	
		try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {}

		/* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        //Schedule a job for the event-dispatching thread:
        //adding TrayIcon.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showGUI();
            }
        });
	}
	
	private static void showGUI()
	{
		if(!SystemTray.isSupported())
		{
			JOptionPane.showMessageDialog(
					null, 
					"This implementation of the Work Hard Play Hard protocol is unsupported by your system!", 
					"Launch failure.",
					JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		
		final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon =
                new TrayIcon(createImage("images/W2HP.gif", "tray icon"));
        final SystemTray tray = SystemTray.getSystemTray();
        
        MenuItem aboutItem = new MenuItem("About W2HP");
        MenuItem joinGame = new MenuItem("Join Game");
        MenuItem exitItem = new MenuItem("Exit");
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(joinGame);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);
         
        try
        	{tray.add(trayIcon);}
        catch (Exception e)
        	{/*Java icon is fine.*/}
          
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,
                        "Work Hard Play Hard uses the power of your computer to support\n"
                      + "services you love at no cost to you!\n"
                      + "\n"
                      + "*electricity not included.");
            }
        });
        joinGame.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		//Open game selection UI
        	}
        });
         
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                System.exit(0);
            }
        });
	}
	
	//Obtain the image URL
    protected static Image createImage(String path, String description) {
        URL imageURL = null;
		try
			{imageURL = new File(path).toURI().toURL();}
		catch (MalformedURLException e)
			{e.printStackTrace();}
         
        if (imageURL == null)
        	return null;
        else
            return (new ImageIcon(imageURL, description)).getImage();
    }
}