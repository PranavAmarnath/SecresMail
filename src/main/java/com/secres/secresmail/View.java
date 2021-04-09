package com.secres.secresmail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.formdev.flatlaf.util.SystemInfo;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

public class View {

	private static JFrame frame;
	private static JXTable mailTable;
	private WebView view;
	private JSplitPane splitPane;
	private JFXPanel contentPanel;
	private JList<Object> attachmentsList;

	public View() {
		createAndShowGUI();
	}

	private void createAndShowGUI() {
		frame = new JFrame("SecresMail");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel mainPanel = new JPanel(new BorderLayout());

		mailTable = new JXTable() {
			private static final long serialVersionUID = 7038819780398948914L;

			@Override
			public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
				if(convertColumnIndexToModel(columnIndex) == 1) {
					return;
				}
				super.changeSelection(rowIndex, columnIndex, toggle, extend);
			}
		};

		//mailTable.setShowGrid(true);
		try {
			mailTable.setAutoCreateRowSorter(true);
		} catch (Exception e) { /* Move on (i.e. ignore sorting if exception occurs) */ }
		mailTable.setCellSelectionEnabled(true);

		JScrollPane tableScrollPane = new JScrollPane(mailTable);

		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(tableScrollPane);

		contentPanel = createJavaFXPanel();

		mailTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -2357302025054207092L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); 
				setBorder(noFocusBorder);
				return this;
			}
		});

		class ForcedListSelectionModel extends DefaultListSelectionModel {

			private static final long serialVersionUID = -8193032676014906509L;

			public ForcedListSelectionModel () {
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			}

			@Override
			public void clearSelection() {
			}

			@Override
			public void removeSelectionInterval(int index0, int index1) {
			}

		}

		mailTable.setSelectionModel(new ForcedListSelectionModel()); // prevent multiple row selection

		mailTable.getColumnModel().setSelectionModel(new DefaultListSelectionModel() {
			private static final long serialVersionUID = 5039886252977060577L;

			@Override
			public boolean isSelectedIndex(int index) {
				return mailTable.convertColumnIndexToModel(index) != 1;
			}
		});

		mailTable.getSelectionModel().setValueIsAdjusting(true); // fire only one ListSelectionEvent

		mailTable.getSelectionModel().addListSelectionListener(e -> {
			if(e.getValueIsAdjusting()) {
				return;
			}
			new Thread(() -> {
				Message message = null;
				Object content = null;
				String email = null;
				try {
					if(!Model.getFolder().isOpen()) {
						Model.getFolder().open(Folder.READ_WRITE);
					}
					message = Model.getMessages()[(Model.getMessages().length - 1) - mailTable.convertRowIndexToModel(mailTable.getSelectedRow())];
					content = message.getContent();
					SwingUtilities.invokeLater(() -> {
						mailTable.getModel().setValueAt(true, mailTable.convertRowIndexToModel(mailTable.getSelectedRow()), 1);
						ListModel<Object> model = (ListModel<Object>) attachmentsList.getModel();
						((DefaultListModel<Object>) model).removeAllElements();
					});
					email = "";
					if(content instanceof Multipart) {
						Multipart mp = (Multipart) content;
						email = getText(mp.getParent());
					}
					else {
						email = message.getContent().toString();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				final String finalEmail = email;

				Platform.runLater(() -> {
					view.getEngine().loadContent(finalEmail);
				});

				int attachmentCount = getAttachmentCount(message);
				if(attachmentCount > 0) {
					try {
						Multipart multipart = (Multipart) message.getContent();

						for(int i = 0; i < multipart.getCount(); i++) {
							//System.out.println("Entered " + i + " file.");

							BodyPart bodyPart = multipart.getBodyPart(i);
							if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
								continue; // dealing with attachments only
							} 
							// do not do this in production code -- a malicious email can easily contain this filename: "../etc/passwd", or any other path: They can overwrite _ANY_ file on the system that this code has write access to!
							File f = new File(bodyPart.getFileName());

							SwingUtilities.invokeLater(() -> {
								((DefaultListModel<Object>) attachmentsList.getModel()).addElement(f);
							});
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}

				//System.out.println(getAttachmentCount(message)); // prints number of attachments
			}).start();
		});

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, contentPanel);
		mainPanel.add(splitPane);

		attachmentsList = new JList<Object>();
		attachmentsList.setModel(new DefaultListModel<Object>());
		attachmentsList.setSelectionModel(new DefaultListSelectionModel() {
			private static final long serialVersionUID = -1175323994925570200L;

			@Override
			public void setAnchorSelectionIndex(final int anchorIndex) { }

			@Override
			public void setLeadAnchorNotificationEnabled(final boolean flag) { }

			@Override
			public void setLeadSelectionIndex(final int leadIndex) { }

			@Override
			public void setSelectionInterval(final int index0, final int index1) { }
		});
		attachmentsList.setCellRenderer(new FileListCellRenderer());

		JScrollPane listScrollPane = new JScrollPane(attachmentsList);

		JPanel attachmentsPanel = new JPanel(new BorderLayout());
		attachmentsPanel.add(listScrollPane);
		attachmentsList.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), new CompoundBorder(new ListCellTitledBorder(attachmentsList, "Attachments"), attachmentsList.getBorder())));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentPanel, attachmentsPanel);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(splitPane);

		this.splitPane.setBottomComponent(bottomPanel);
		setDividerLocation(this.splitPane, 0.5);
		this.splitPane.setResizeWeight(0.5);

		setDividerLocation(splitPane, 0.75);
		splitPane.setResizeWeight(0.75);

		frame.add(mainPanel);

		frame.pack();
	}

	private void setDividerLocation(final JSplitPane splitter, final double proportion) {
		if (splitter.isShowing()) {
			if ((splitter.getWidth() > 0) && (splitter.getHeight() > 0)) {
				splitter.setDividerLocation(proportion);
			} else {
				splitter.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent ce) {
						splitter.removeComponentListener(this);
						setDividerLocation(splitter, proportion);
					}
				});
			}
		} else {
			splitter.addHierarchyListener(new HierarchyListener() {
				@Override
				public void hierarchyChanged(HierarchyEvent e) {
					if (((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) && splitter.isShowing()) {
						splitter.removeHierarchyListener(this);
						setDividerLocation(splitter, proportion);
					}
				}
			});
		}
	}

	private JFXPanel createJavaFXPanel() {
		JFXPanel contentPanel = new JFXPanel();

		Platform.runLater(() -> {
			view = new WebView();

			StackPane root = new StackPane();
			root.getChildren().add(view);
			if(SystemInfo.isMacOS) root.getStylesheets().add("/style_mac.css");
			else root.getStylesheets().add("/style_win.css");

			contentPanel.setScene(new Scene(root));
		});

		return contentPanel;
	}

	private int getAttachmentCount(Message message) {
		int count = 0;
		try {
			Object object = message.getContent();
			if(object instanceof Multipart) {
				Multipart parts = (Multipart) object;
				for(int i = 0; i < parts.getCount(); ++i) {
					MimeBodyPart part = (MimeBodyPart) parts.getBodyPart(i);
					if(Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
						++count;
				}
			}
		} catch (IOException | MessagingException e) {
			e.printStackTrace();
		}
		return count;
	}

	/**
	 * Return the primary text content of the message.
	 * 
	 * @param p  the <code>Part</code>
	 * @return String  the primary text content
	 */
	private String getText(Part p) throws MessagingException, IOException {
		if(p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			return s;
		}

		if(p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for(int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if(bp.isMimeType("text/plain")) {
					if(text == null)
						text = getText(bp);
					continue;
				}
				else if(bp.isMimeType("text/html")) {
					String s = getText(bp);
					if(s != null)
						return s;
				}
				else {
					return getText(bp);
				}
			}
			return text;
		}
		else if(p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart)p.getContent();
			for(int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if(s != null)
					return s;
			}
		}

		return null;
	}

	public static JFrame getFrame() {
		return frame;
	}

	public static JTable getTable() {
		return mailTable;
	}

}
