package de.mastacheata.vor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JComboBox;

/**
 * Backup files from one selected category of SAM Broadcaster to a selected Harddisk directory 
 * 
 * @author benedikt
 *
 */
public class BackupSamCategories extends JFrame {
	private static final long serialVersionUID = 1L;
	
	/**
	 * JDBC Connection for MySQL Database
	 */
	private Connection connect;
	
	/**
	 * Backup Target
	 */
	private File targetDirectory;
	
	/**
	 * Database connection specifics
	 */
	private JPanel dbPanel;
	private final JLabel userLabel = new JLabel();
	private final JTextField user = new JTextField();
	private final JLabel passLabel = new JLabel();
	private final JPasswordField pass = new JPasswordField();
	private final JLabel dbNameLabel = new JLabel();
	private final JTextField dbName = new JTextField();
	private final JButton selectDb = new JButton();
	
	/**
	 * Open Database when credentials were entered
	 * Redraws interface and adds the possible categories in a combobox
	 * Also adds a FileBrowser to select the Target Directory
	 */
	private final ActionListener selectDbListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String user = BackupSamCategories.this.user.getText();
			String pass = new String(BackupSamCategories.this.pass.getPassword());
			String dbName = BackupSamCategories.this.dbName.getText();
			
			try {
				String[] categories = BackupSamCategories.this.getCategorynames(user, pass, dbName);
				BackupSamCategories.this.remove(BackupSamCategories.this.dbPanel);
				BackupSamCategories.this.drawBackupPanel(categories);
				BackupSamCategories.this.add(BackupSamCategories.this.backupPanel);
				BackupSamCategories.this.repaint();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	};
	
	private void drawDatabasePanel() {
		dbPanel = new JPanel();
		dbPanel.setSize(800, 600);
		dbPanel.setLayout(null);
		
		this.userLabel.setLabelFor(this.user);
		this.userLabel.setBounds(170, 120, 120, 30);
		this.userLabel.setText("Database Username:");
		this.user.setBounds(300, 120, 150, 30);
		this.user.setText("root");
		dbPanel.add(userLabel);
		dbPanel.add(user);
		
		this.passLabel.setLabelFor(this.pass);
		this.passLabel.setBounds(170, 160, 120, 30);
		this.passLabel.setText("Database Password:");
		this.pass.setBounds(300, 160, 150, 30);
		this.pass.setText("ehosica");
		dbPanel.add(passLabel);
		dbPanel.add(pass);
		
		this.dbNameLabel.setLabelFor(this.dbName);
		this.dbNameLabel.setBounds(170, 200, 120, 30);
		this.dbNameLabel.setText("Database Name:");
		this.dbName.setBounds(300, 200, 150, 30);
		this.dbName.setText("samdb_mysql");
		dbPanel.add(dbNameLabel);
		dbPanel.add(dbName);
		
		this.selectDb.setText("Get Category List");
		this.selectDb.setBounds(300, 500, 200, 40);
		this.selectDb.addActionListener(selectDbListener);
		this.getRootPane().setDefaultButton(selectDb);
		dbPanel.add(selectDb);
	}
	
	
	private JPanel backupPanel;
	private final JLabel categoryLabel = new JLabel();
	private JComboBox<String> categorySelector;
	private final JLabel browserLabel = new JLabel();
	private final JButton browseTarget = new JButton();
	private final JFileChooser fileBrowser = new JFileChooser();
	private final JButton confirmButton = new JButton();
	
	private final ActionListener browseListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int returnVal = fileBrowser.showOpenDialog(BackupSamCategories.this);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				targetDirectory = fileBrowser.getSelectedFile();
			}
		}
	};
	
	private final ActionListener backupListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			BackupSamCategories.this.startBackup((String) BackupSamCategories.this.categorySelector.getSelectedItem());
		}
	};

	private void drawBackupPanel(String[] categories) {
		backupPanel = new JPanel();
		backupPanel.setSize(800, 600);
		backupPanel.setLayout(null);
		
		this.categoryLabel.setLabelFor(categorySelector);
		this.categoryLabel.setBounds(170, 120, 120, 30);
		this.categoryLabel.setText("Category for Backup:");
		this.categorySelector = new JComboBox<String>(categories);
		this.categorySelector.setBounds(300, 120, 180, 30);
		backupPanel.add(categoryLabel);
		backupPanel.add(categorySelector);
		
		this.browserLabel.setLabelFor(browseTarget);
		this.browserLabel.setBounds(170, 160, 120, 30);
		this.browserLabel.setText("Target Directory:");
		this.browseTarget.setBounds(300, 160, 150, 130);
		this.browseTarget.setText("Browse...");
		this.browseTarget.addActionListener(browseListener);
		this.fileBrowser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		backupPanel.add(browserLabel);
		backupPanel.add(browseTarget);
		
		this.confirmButton.setText("Start Backup");
		this.confirmButton.setBounds(300, 500, 200, 40);
		this.confirmButton.addActionListener(backupListener);
		this.getRootPane().setDefaultButton(confirmButton);
		backupPanel.add(confirmButton);
	}
	
	public BackupSamCategories() throws HeadlessException {
		this.setTitle("Backup SAM Categories to Harddisk folders");
		this.setSize(800, 600);

		this.drawDatabasePanel();
		this.add(dbPanel);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
	}
	
	private String[] getCategorynames(String user, String pass, String dbName) throws SQLException {
		connect = DriverManager.getConnection("jdbc:mysql://localhost/"+dbName+"?user="+user+"&password="+pass);
		
		Statement query = connect.createStatement();
		ResultSet result = query.executeQuery("SELECT name FROM category");
		
		Vector<String> categories = new Vector<String>();
		
		while (result.next()) {
			categories.add(result.getString("name"));
		}
		result.close();
		query.close();
				
		return (String[]) categories.toArray(new String[categories.size()]);
	}
	
	private void startBackup(String category)
	{	
		try {
			Statement addBackupStatus = connect.createStatement();
			// If it fails, the column already exists and there is no need to take separate action
			addBackupStatus.execute("ALTER TABLE songlist ADD COLUMN backupstatus TINYINT(1)");
			addBackupStatus.close();
			
			Statement query = connect.createStatement();
			ResultSet result = query.executeQuery("SELECT filename FROM songlist JOIN categorylist ON (songlist.ID = categorylist.songID) JOIN category ON (categorylist.categoryID = category.ID) WHERE songlist.backupstatus = 0 AND category.name ='" + category + "'");
			String filepath, target;
			
			while (result.next()) {
				filepath = result.getString("filename").replace('\\', '/');
				
				FileInputStream source = new FileInputStream(filepath);
				target = (targetDirectory.getAbsolutePath() + '\\' + new File(filepath).getName()).replace('\\', '/');
				
				FileOutputStream destination = new FileOutputStream(target);
				FileChannel sourceFileChannel = source.getChannel();
		        FileChannel destinationFileChannel = destination.getChannel();
		        
				long size = sourceFileChannel.size();
				sourceFileChannel.transferTo(0, size, destinationFileChannel);
				
				Statement setBackupStatus = connect.createStatement();
				setBackupStatus.execute("UPDATE songlist SET backupstatus = 1 WHERE filename = " + result.getString("filename"));
				setBackupStatus.close();
			}	
			
			result.close();
			query.close();
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}	
		JOptionPane.showMessageDialog(this, "Backup of category " + category + " finished.");
	}
	
	public static void main(String[] args) throws Exception {
		new BackupSamCategories();
	}
}
