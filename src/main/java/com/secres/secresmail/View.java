package com.secres.secresmail;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXStatusBar.Constraint;

import com.formdev.flatlaf.util.SystemInfo;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class View {

	private static JFrame frame;
	private static JTable mailTable;
	private JProgressBar readProgressBar;

	public View() {
		createAndShowGUI();
	}

	private void createAndShowGUI() {
		frame = new JFrame("SecresMail");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel mainPanel = new JPanel(new BorderLayout());

		mailTable = new JTable() {
			@Override
			public boolean isCellEditable(int row, int col) {
				switch (col) {
				case 1:
					return true;
				default:
					return false;
				}
			}
		};

		mailTable.setShowGrid(true);
		try {
			mailTable.setAutoCreateRowSorter(true);
		} catch (Exception e) { /* Move on (i.e. ignore sorting if exception occurs) */ }

		JScrollPane tableScrollPane = new JScrollPane(mailTable);

		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(tableScrollPane);

		JFXPanel contentPanel = new JFXPanel();

		mailTable.getSelectionModel().addListSelectionListener(e -> {
			new Thread(() -> {
				Message message;
				Object content;
				BodyPart bp;
				String email = null;
				try {
					if(!Model.getFolder().isOpen()) {
						Model.getFolder().open(Folder.READ_WRITE);
					}
					message = Model.getMessages()[(Model.getMessages().length - 1) - (int) mailTable.getSelectedRow()];
					content = message.getContent();
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
					WebView view = new WebView();
					WebEngine engine = view.getEngine();

					StackPane root = new StackPane();
					root.getChildren().add(view);
					if(SystemInfo.isMacOS) root.getStylesheets().add("/style_mac.css");
					else root.getStylesheets().add("/style_win.css");

					engine.loadContent(finalEmail);

					contentPanel.setScene(new Scene(root));
				});
			}).start();
		});

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, contentPanel);
		mainPanel.add(splitPane);

		setDividerLocation(splitPane, 0.5);
		splitPane.setResizeWeight(0.5);

		frame.add(mainPanel);

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

	public static JFrame getFrame() {
		return frame;
	}

	public static JTable getTable() {
		return mailTable;
	}

}
