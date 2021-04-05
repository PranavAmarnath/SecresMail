package com.secres.secresmail;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.auth.LoginEvent;

import com.sun.mail.imap.IMAPFolder;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromStringTerm;

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
									emailFolder.setFlags(new Message[]{messages[(messages.length - 1) - row]}, new Flags(Flags.Flag.SEEN), true);
									//System.out.println(getMessages()[(getMessages().length - 1) - row].isSet(Flags.Flag.SEEN));
								} catch (MessagingException e) {
									e.printStackTrace();
								}
							}
							else if((Boolean) this.getValueAt(row, col) == false) {
								try {
									emailFolder.setFlags(new Message[]{messages[(messages.length - 1) - row]}, new Flags(Flags.Flag.SEEN), false);
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

			Store store = emailSession.getStore("imaps");

			try {
				store.connect(host, 993, user, password);
			} catch (Exception e) {
				e.printStackTrace();
				Main.getLoginListener().loginFailed(new LoginEvent(e));
				return;
			}

			View.getFrame().setVisible(true);

			//create the folder object and open it
			emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_WRITE);

			// retrieve the messages from the folder in an array and print it
			messages = emailFolder.getMessages();
			
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						//System.out.println("TimerTask started");
						if(!emailFolder.isOpen()) emailFolder.open(Folder.READ_WRITE);
						// We do the first reading of emails
						int start = 1;
						int end = emailFolder.getMessageCount();
						while(start <= end) {
							// new messages that have arrived
							start = end + 1;
							end = emailFolder.getMessageCount();
						}

						// Adding a MessageCountListener to "listen" to new messages
						emailFolder.addMessageCountListener(new MessageCountAdapter() {
							@Override
							public void messagesAdded(MessageCountEvent ev) {
								try {
									//System.out.println("Message received");
									Message[] msgs = emailFolder.search(new AndTerm(new FlagTerm(new Flags(Flags.Flag.SEEN), false), new FromStringTerm("@gmail.com")));
									for(Message message : msgs) {
										model.insertRow(0, new Object[] {message.getSubject(), message.isSet(Flags.Flag.SEEN), message.getFrom()[0], new SimpleDateFormat().format(message.getSentDate())});
										messages = emailFolder.getMessages(); // update messages array length
									}
								} catch (MessagingException ex) {
									ex.printStackTrace();
								}
							}
						});

						// Waiting for new messages
						for(;;) {
							((IMAPFolder) emailFolder).idle();
						}
					} catch (MessagingException ex) {
						ex.printStackTrace();
					}

				}
			}, 0, 100000); // 0 milliseconds till start, 100 seconds delay between checks

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
