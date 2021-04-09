package com.secres.secresmail;

import java.awt.Desktop;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXLoginPane;
import org.jdesktop.swingx.auth.LoginAdapter;
import org.jdesktop.swingx.auth.LoginEvent;
import org.jdesktop.swingx.auth.LoginListener;
import org.jdesktop.swingx.auth.LoginService;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

public class Main {

	private static LoginListener loginListener;
	private static JXLoginPane loginPane;

	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.application.name", "SecresMail");
		System.setProperty("apple.awt.application.appearance", "system");
		System.setProperty("apple.awt.antialiasing", "true");
		System.setProperty("apple.awt.textantialiasing", "true");

		if(SystemInfo.isMacOS) {
			try {
				SwingUtilities.invokeLater(() -> {
					Desktop desktop = Desktop.getDesktop();

					desktop.setAboutHandler(e -> {
						JOptionPane.showMessageDialog(View.getFrame(), "About Dialog", "About SecresMail",
								JOptionPane.PLAIN_MESSAGE);
					});
					desktop.setPreferencesHandler(e -> {
						JOptionPane.showMessageDialog(View.getFrame(), "Preferences", "Preferences",
								JOptionPane.INFORMATION_MESSAGE);
					});
					desktop.setQuitHandler((e, r) -> {
						System.exit(0);
					});
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		SwingUtilities.invokeLater(() -> {
			FlatLightLaf.install();

			final String host = "imap.googlemail.com"; // change accordingly

			loginPane = new JXLoginPane();
			createLoginDialog();

			LoginService loginService = new LoginService() {
				@Override
				public boolean authenticate(String name, char[] password, String server) throws Exception {
					return true;
				}
			};

			loginListener = new LoginAdapter() {
				@Override
				public void loginFailed(LoginEvent source) {
					loginPane = new JXLoginPane();

					LoginService loginService = new LoginService() {
						@Override
						public boolean authenticate(String name, char[] password, String server) throws Exception {
							return true;
						}
					};
					loginService.addLoginListener(this);
					loginPane.setLoginService(loginService);

					createLoginDialog();
				}

				@Override
				public void loginSucceeded(LoginEvent source) {
					new View();
					new Model(host, loginPane.getUserName(), String.valueOf(loginPane.getPassword()));
				}
			};

			loginService.addLoginListener(loginListener);
			loginPane.setLoginService(loginService);

			// if loginPane was cancelled or closed then its status is CANCELLED
			// and still need to dispose main JFrame to exiting application
			if(loginPane.getStatus() == JXLoginPane.Status.CANCELLED) {
				View.getFrame().dispatchEvent(new WindowEvent(View.getFrame(), WindowEvent.WINDOW_CLOSING));
			}
		});
	}

	private static void createLoginDialog() {
		JXLoginPane.JXLoginFrame dialog = new JXLoginPane.JXLoginFrame(loginPane);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		dialog.setVisible(true);
	}

	public static LoginListener getLoginListener() {
		return loginListener;
	}

}
