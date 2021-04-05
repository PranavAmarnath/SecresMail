package com.secres.secresmail;

import java.text.SimpleDateFormat;
import java.util.Properties;

import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.auth.LoginEvent;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;

public class Model {

	private static DefaultTableModel model;
	private String[] header;
	private static Message[] messages;
	private static Folder emailFolder;

	public Model(final String host, final String user, final String password) {
		class Worker extends SwingWorker<Void, String> {
			@Override
			protected Void doInBackground() {
				header = new String[]{"Subject", "Read", "Correspondents", "Date"};

				model = new DefaultTableModel(header, 0) {
					@Override
					public Class<?> getColumnClass(int columnIndex) {
						Class clazz = String.class;
						switch (columnIndex) {
						case 1:
							clazz = Boolean.class;
							break;
						}
						return clazz;
					}
					
					@Override
					public void setValueAt(Object value, int row, int col) {
						super.setValueAt(value, row, col);
						if(col == 1) {
							if((Boolean) this.getValueAt(row, col) == true) {
								try {
									emailFolder.setFlags(new Message[]{getMessages()[(getMessages().length - 1) - row]}, new Flags(Flags.Flag.SEEN), true);
									//System.out.println(getMessages()[(getMessages().length - 1) - row].isSet(Flags.Flag.SEEN));
								} catch (MessagingException e) {
									e.printStackTrace();
								}
							}
							else if((Boolean) this.getValueAt(row, col) == false) {
								try {
									emailFolder.setFlags(new Message[]{getMessages()[(getMessages().length - 1) - row]}, new Flags(Flags.Flag.SEEN), false);
									//System.out.println(getMessages()[(getMessages().length - 1) - row].isSet(Flags.Flag.SEEN));
								} catch (MessagingException e) {
									e.printStackTrace();
								}
							}
						}   
					}
				};
				View.getTable().setModel(model);

				readMail(host, user, password);
				return null;
			}
		}
		new Worker().execute();
	}

	private void readMail(String host, String user, String password) {
		try {
			//create properties field
			Properties properties = new Properties();

			Session emailSession = Session.getDefaultInstance(properties);

			//create the POP3 store object and connect with the pop server
			Store store = emailSession.getStore("imaps");

			try {
				store.connect(host, 993, user, password);
			} catch (Exception e) {
				Main.getLoginListener().loginFailed(new LoginEvent(e));
				return;
			}

			View.getFrame().setVisible(true);

			//create the folder object and open it
			emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_WRITE);

			// retrieve the messages from the folder in an array and print it
			messages = emailFolder.getMessages();

			for(int i = messages.length - 1; i >= 0; i--) {
				Message message = messages[i];
				model.addRow(new Object[] {message.getSubject(), message.isSet(Flags.Flag.SEEN), message.getFrom()[0], new SimpleDateFormat().format(message.getSentDate())});
			}

			//close the store and folder objects
			emailFolder.close(false);
			//store.close();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	public static Message[] getMessages() {
		return messages;
	}

	public static DefaultTableModel getModel() {
		return model;
	}

	public static Folder getFolder() {
		return emailFolder;
	}

}
