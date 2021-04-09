package com.secres.secresmail;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

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
import jakarta.mail.event.ConnectionAdapter;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;

public class Model {

	private static DefaultTableModel model;
	private String[] header;
	private static Message[] messages;
	private static Folder emailFolder;
	private int IMAPS_PORT = 993;
	private static Session emailSession;
	private static Properties properties;

	public Model(final String host, final String user, final String password) {
		class Worker extends SwingWorker<Void, String> {
			@Override
			protected Void doInBackground() {
				header = new String[] { "Subject", "Read", "Correspondents", "Date" };

				model = new DefaultTableModel(header, 0) {
					private static final long serialVersionUID = -2116346605141053545L;

					@Override
					public Class<?> getColumnClass(int columnIndex) {
						Class<?> clazz = String.class;
						switch(columnIndex) {
						case 1:
							clazz = Boolean.class;
							break;
						case 3:
							clazz = Date.class;
						}
						return clazz;
					}

					@Override
					public boolean isCellEditable(int row, int col) {
						switch(col) {
						case 1:
							return true;
						default:
							return false;
						}
					}

					@Override
					public void setValueAt(Object value, int row, int col) {
						super.setValueAt(value, row, col);
						if(!emailFolder.isOpen()) {
							try {
								emailFolder.open(Folder.READ_WRITE);
							} catch (MessagingException e) {
								e.printStackTrace();
							}
						}
						if(col == 1) {
							if((Boolean) this.getValueAt(row, col) == true) {
								try {
									emailFolder.setFlags(new Message[] { messages[(messages.length - 1) - row] },
											new Flags(Flags.Flag.SEEN), true);
									// System.out.println(getMessages()[(getMessages().length - 1) -
									// row].isSet(Flags.Flag.SEEN));
								} catch (MessagingException e) {
									e.printStackTrace();
								}
							}
							else if((Boolean) this.getValueAt(row, col) == false) {
								try {
									emailFolder.setFlags(new Message[] { messages[(messages.length - 1) - row] },
											new Flags(Flags.Flag.SEEN), false);
									// System.out.println(getMessages()[(getMessages().length - 1) -
									// row].isSet(Flags.Flag.SEEN));
								} catch (MessagingException e) {
									e.printStackTrace();
								}
							}
						}
					}
				};
				View.getTable().setModel(model);
				View.getTable().getColumnModel().getColumn(1).setMaxWidth(50);

				TableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {
					private static final long serialVersionUID = -7189272880275372668L;

					public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						if(value instanceof Date) {
							value = new SimpleDateFormat().format(value);
						}
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
						setBorder(noFocusBorder);
						return this;
					}
				};

				View.getTable().getColumnModel().getColumn(3).setCellRenderer(tableCellRenderer); // for correct sorting

				readMail(host, user, password);
				return null;
			}
		}
		new Worker().execute();
	}

	private void readMail(String host, String user, String password) {
		try {
			// create properties field
			properties = new Properties();
			properties.setProperty("mail.imaps.partialfetch", "false");
			properties.setProperty("mail.smtp.ssl.enable", "true");
			properties.setProperty("mail.user", user);
			properties.setProperty("mail.password", password);
			properties.setProperty("mail.smtp.host", "smtp.gmail.com");
			properties.setProperty("mail.smtp.port", "465");
			properties.setProperty("mail.smtp.auth", "true");

			emailSession = Session.getDefaultInstance(properties);

			Store store = emailSession.getStore("imaps");

			try {
				store.connect(host, IMAPS_PORT, user, password);
			} catch (Exception e) {
				e.printStackTrace();
				Main.getLoginListener().loginFailed(new LoginEvent(e));
				return;
			}

			View.getFrame().setVisible(true);

			// create the folder object and open it
			emailFolder = store.getFolder("INBOX");
			emailFolder.open(Folder.READ_WRITE);

			// retrieve the messages from the folder in an array and print it
			messages = emailFolder.getMessages();

			emailFolder.addConnectionListener(new ConnectionAdapter() {
				@Override
				public void closed(ConnectionEvent e) {
					try {
						emailFolder.open(Folder.READ_WRITE);
					} catch (MessagingException e1) {
						e1.printStackTrace();
					}
				}
			});

			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						// System.out.println("TimerTask started");
						if(!emailFolder.isOpen()) {
							emailFolder.open(Folder.READ_WRITE);
						}
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
									// System.out.println("Message added");
									if(!emailFolder.isOpen()) {
										emailFolder.open(Folder.READ_WRITE);
									}
									Message[] msgs = ev.getMessages();
									for(Message message : msgs) {
										model.insertRow(0,
												new Object[] { message.getSubject(), message.isSet(Flags.Flag.SEEN),
														message.getFrom()[0],
														new SimpleDateFormat().format(message.getSentDate()) });
										messages = emailFolder.getMessages(); // update messages array length
									}
								} catch (MessagingException ex) {
									ex.printStackTrace();
								}
							}

							@Override
							public void messagesRemoved(MessageCountEvent ev) {
								try {
									// System.out.println("Message removed");
									if(!emailFolder.isOpen()) {
										emailFolder.open(Folder.READ_WRITE);
									}
									Message[] msgs = ev.getMessages();
									for(Message message : msgs) {
										// System.out.println((messages.length - 1) -
										// Arrays.asList(messages).indexOf(message));
										model.removeRow(
												(messages.length - 1) - Arrays.asList(messages).indexOf(message));
										emailFolder.expunge();
										messages = emailFolder.getMessages(); // update messages array length
									}
								} catch (MessagingException ex) {
									ex.printStackTrace();
								}
							}
						});

						// Waiting for new messages
						for(;;) {
							try {
								((IMAPFolder) emailFolder).idle();
							} catch (FolderClosedException e) {
								e.printStackTrace();
								if(!emailFolder.isOpen()) {
									emailFolder.open(Folder.READ_WRITE);
								}
							}
						}
					} catch (MessagingException ex) {
						ex.printStackTrace();
					}
				}
			}, 0, 100000); // 0 milliseconds till start, 100 seconds delay between checks

			for(int i = messages.length - 1; i >= 0; i--) {
				if(!emailFolder.isOpen()) {
					emailFolder.open(Folder.READ_WRITE);
				}
				Message message = messages[i];
				model.addRow(new Object[] { message.getSubject(), message.isSet(Flags.Flag.SEEN), message.getFrom()[0],
						message.getSentDate() });
			}

			// close the store and folder objects
			// emailFolder.close(false);
			// store.close();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	public static Properties getProperties() {
		return properties;
	}

	public static Session getSession() {
		return emailSession;
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
