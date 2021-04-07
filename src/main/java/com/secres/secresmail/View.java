package com.secres.secresmail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXStatusBar.Constraint;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class View {

	private static JFrame frame;
	private static JXTable mailTable;
	private JProgressBar readProgressBar;
	private WebView view;

	public View() {
		createAndShowGUI();
	}

	private void createAndShowGUI() {
		frame = new JFrame("SecresMail");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel mainPanel = new JPanel(new BorderLayout());

		mailTable = new JXTable() {
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

		JFXPanel contentPanel = new JFXPanel();

		Platform.runLater(() -> {
			view = new WebView();
			WebEngine engine = view.getEngine();

			StackPane root = new StackPane();
			root.getChildren().add(view);
			if(SystemInfo.isMacOS) root.getStylesheets().add("/style_mac.css");
			else root.getStylesheets().add("/style_win.css");

			contentPanel.setScene(new Scene(root));
		});

		mailTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); 
				setBorder(noFocusBorder);
				return this;
			}
		});

		class ForcedListSelectionModel extends DefaultListSelectionModel {

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
					SwingUtilities.invokeLater(() -> mailTable.getModel().setValueAt(true, mailTable.getSelectedRow(), 1));
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

				//System.out.println(getAttachmentCount(message)); // prints number of attachments
			}).start();
		});

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, contentPanel);
		mainPanel.add(splitPane);

		setDividerLocation(splitPane, 0.5);
		splitPane.setResizeWeight(0.5);

		frame.add(mainPanel);

		/*
		JXStatusBar statusBar = new JXStatusBar();
		JLabel statusLabel = new JLabel("Ready");
		JXStatusBar.Constraint c1 = new Constraint();
		c1.setFixedWidth(100);
		statusBar.add(statusLabel, c1); // Fixed width of 100 with no inserts
		JXStatusBar.Constraint c2 = new Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL); // Fill with no inserts
		readProgressBar = new JProgressBar();
		//readProgressBar.setIndeterminate(true);
		statusBar.add(readProgressBar, c2); // Fill with no insets - will use remaining space

		frame.add(statusBar, BorderLayout.SOUTH);
		*/

		frame.pack();
	}

	public static void setDividerLocation(final JSplitPane splitter, final double proportion) {
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

	public static JFrame getFrame() {
		return frame;
	}

	public static JTable getTable() {
		return mailTable;
	}

}
