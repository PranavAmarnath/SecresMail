package com.secres.secresmail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang3.StringUtils;
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
				BodyPart bp;
				String email = null;
				try {
					if(!Model.getFolder().isOpen()) {
						Model.getFolder().open(Folder.READ_WRITE);
					}
					message = Model.getMessages()[(Model.getMessages().length - 1) - mailTable.getSelectedRow()];
					content = message.getContent();
					SwingUtilities.invokeLater(() -> {
						mailTable.getModel().setValueAt(true, mailTable.getSelectedRow(), 1);
						ListModel<Object> model = (ListModel<Object>) attachmentsList.getModel();
						((DefaultListModel<Object>) model).removeAllElements();
					});
					bp = null;
					email = "";
					if(content instanceof Multipart) {
						Multipart mp = (Multipart) content;
						for(int i = 0; i < mp.getCount(); i++) {
							bp = mp.getBodyPart(i);
							if(Pattern
									.compile(Pattern.quote("text/html"),
											Pattern.CASE_INSENSITIVE)
									.matcher(bp.getContentType()).find()) {
								// found html part
								//System.out.println((String) bp.getContent());
								email = (String) bp.getContent();
								break;
							}
						}
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
							if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
									StringUtils.isBlank(bodyPart.getFileName())) {
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
			if (object instanceof Multipart) {
				Multipart parts = (Multipart) object;
				for (int i = 0; i < parts.getCount(); ++i) {
					MimeBodyPart part = (MimeBodyPart) parts.getBodyPart(i);
					if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
						++count;
				}
			}
		} catch (IOException | MessagingException e) {
			e.printStackTrace();
		}
		return count;
	}

	/** A FileListCellRenderer for a File. */
	private class FileListCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = -7799441088157759804L;
		private FileSystemView fileSystemView;
		private JLabel label;
		private Color textSelectionColor = Color.BLACK;
		private Color backgroundSelectionColor = Color.CYAN;
		private Color textNonSelectionColor = Color.BLACK;
		private Color backgroundNonSelectionColor = Color.WHITE;

		public FileListCellRenderer() {
			label = new JLabel();
			label.setOpaque(true);
			fileSystemView = FileSystemView.getFileSystemView();
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,	boolean expanded) {
			File file = (File) value;
			if(fileSystemView.getSystemIcon(file) != null ) {
				label.setIcon(fileSystemView.getSystemIcon(file));
			}
			else {
				label.setIcon(UIManager.getIcon("Tree.leafIcon"));
			}
			label.setText(fileSystemView.getSystemDisplayName(file));
			label.setToolTipText(file.getPath());

			if (selected) {
				label.setBackground(backgroundSelectionColor);
				label.setForeground(textSelectionColor);
			} else {
				label.setBackground(backgroundNonSelectionColor);
				label.setForeground(textNonSelectionColor);
			}

			return label;
		}

	}

	public static JFrame getFrame() {
		return frame;
	}

	public static JTable getTable() {
		return mailTable;
	}

}
