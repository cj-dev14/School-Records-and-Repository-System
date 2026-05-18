//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package lab4;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class lab4 extends JFrame {
    private static final String DB_URL = "jdbc:mysql://hopper.proxy.rlwy.net:19507/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=60000&socketTimeout=60000";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "oXgdZdeMhQaPQGiFmxSIHuiQCNeCamQr";
    private static final Color PRIMARY = new Color(25, 84, 163);
    private static final Color SECONDARY = new Color(240, 245, 255);
    private static final Color ACCENT = new Color(220, 53, 69);
    private static final Color SUCCESS = new Color(40, 167, 69);
    private static final Color TABLE_HDR = new Color(25, 84, 163);
    private static final Color WHITE;
    private static final Color GOLD;
    private static int currentUserID;
    private static String currentUsername;
    private static Role currentRole;
    private JTabbedPane tabs;
    private Connection conn;
    private JLabel sessionLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception var1) {
            }

            (new lab4()).showLoginDialog();
        });
    }

    public lab4() {
        this.setTitle("School Records & Repository Management System");
        this.setDefaultCloseOperation(3);
        this.setSize(1280, 780);
        this.setLocationRelativeTo((Component)null);
        this.connectDB();
        this.ensureAuthTables();
    }

    private void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (Exception var2) {
            Exception e = var2;
            JOptionPane.showMessageDialog((Component)null, "Database connection failed:\n" + e.getMessage(), "Connection Error", 0);
        }

    }

    private Connection getConn() throws SQLException {
        if (this.conn == null || this.conn.isClosed()) {
            this.connectDB();
        }

        return this.conn;
    }

    private void ensureAuthTables() {
        String createUsers = "CREATE TABLE IF NOT EXISTS SystemUser (  UserID       INT AUTO_INCREMENT PRIMARY KEY,  Username     VARCHAR(50)  UNIQUE NOT NULL,  PasswordHash VARCHAR(256) NOT NULL,  Salt         VARCHAR(64)  NOT NULL,  Role         ENUM('ADMIN','TEACHER','STUDENT') DEFAULT 'STUDENT',  FullName     VARCHAR(100),  Email        VARCHAR(100),  CreatedAt    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  LastLogin    TIMESTAMP NULL)";
        String createAudit = "CREATE TABLE IF NOT EXISTS AuditLog (  LogID      INT AUTO_INCREMENT PRIMARY KEY,  UserID     INT,  Username   VARCHAR(50),  Action     VARCHAR(20),  TableName  VARCHAR(50),  RecordID   VARCHAR(50),  Details    TEXT,  ActionTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  FOREIGN KEY (UserID) REFERENCES SystemUser(UserID))";

        try {
            Statement st = this.getConn().createStatement();

            try {
                st.execute(createUsers);
                st.execute(createAudit);
                Object count = this.scalarQuery("SELECT COUNT(*) FROM SystemUser WHERE Role='ADMIN'");
                if (count != null && ((Number)count).intValue() == 0) {
                    String[] saltAndHash = this.hashPassword("admin123");
                    this.executeUpdate("INSERT INTO SystemUser(Username,PasswordHash,Salt,Role,FullName,Email) VALUES(?,?,?,?,?,?)", "admin", saltAndHash[1], saltAndHash[0], "ADMIN", "System Administrator", "admin@school.edu");
                }
            } catch (Throwable var7) {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (st != null) {
                st.close();
            }
        } catch (Exception var8) {
            Exception e = var8;
            JOptionPane.showMessageDialog((Component)null, "Auth table setup failed:\n" + e.getMessage());
        }

    }

    private String[] hashPassword(String plaintext) {
        try {
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[16];
            rng.nextBytes(salt);
            String hexSalt = this.bytesToHex(salt);
            String hexHash = this.pbkdf2(plaintext.toCharArray(), salt);
            return new String[]{hexSalt, hexHash};
        } catch (Exception var6) {
            Exception e = var6;
            throw new RuntimeException("Hashing failed: " + e.getMessage());
        }
    }

    private boolean verifyPassword(String plaintext, String hexSalt, String storedHash) {
        try {
            byte[] salt = this.hexToBytes(hexSalt);
            String computedHash = this.pbkdf2(plaintext.toCharArray(), salt);
            return computedHash.equals(storedHash);
        } catch (Exception var6) {
            return false;
        }
    }

    private String pbkdf2(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return this.bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        byte[] var3 = bytes;
        int var4 = bytes.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            byte b = var3[var5];
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }

        return data;
    }

    private void audit(String action, String tableName, String recordID, String details) {
        try {
            this.executeUpdate("INSERT INTO AuditLog(UserID,Username,Action,TableName,RecordID,Details) VALUES(?,?,?,?,?,?)", currentUserID, currentUsername, action, tableName, recordID, details);
        } catch (Exception var6) {
        }

    }

    private void showLoginDialog() {
        JDialog dlg = new JDialog((Frame)null, "Login – School Records System", true);
        dlg.setSize(420, 340);
        dlg.setLocationRelativeTo((Component)null);
        dlg.setDefaultCloseOperation(2);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SECONDARY);
        panel.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = 2;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        JLabel titleLbl = new JLabel("School Records System", 0);
        titleLbl.setFont(new Font("Segoe UI", 1, 18));
        titleLbl.setForeground(PRIMARY);
        panel.add(titleLbl, gc);
        gc.gridy = 1;
        JLabel subLbl = new JLabel("Please log in to continue", 0);
        subLbl.setFont(new Font("Segoe UI", 0, 12));
        subLbl.setForeground(Color.GRAY);
        panel.add(subLbl, gc);
        gc.gridwidth = 1;
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        panel.add(new JLabel("Username:"), gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        JTextField userField = new JTextField(18);
        panel.add(userField, gc);
        gc.gridy = 3;
        gc.gridx = 0;
        gc.weightx = 0.0;
        panel.add(new JLabel("Password:"), gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        JPasswordField passField = new JPasswordField(18);
        panel.add(passField, gc);
        gc.gridy = 4;
        gc.gridx = 0;
        gc.gridwidth = 2;
        JLabel msgLbl = new JLabel(" ", 0);
        msgLbl.setForeground(ACCENT);
        msgLbl.setFont(new Font("Segoe UI", 0, 11));
        panel.add(msgLbl, gc);
        gc.gridy = 5;
        JPanel btnRow = new JPanel(new FlowLayout(1, 12, 0));
        btnRow.setBackground(SECONDARY);
        JButton loginBtn = this.btn("Login", PRIMARY);
        JButton registerBtn = this.btn("Register", SUCCESS);
        btnRow.add(loginBtn);
        btnRow.add(registerBtn);
        panel.add(btnRow, gc);
        ActionListener doLogin = (e) -> {
            String uname = userField.getText().trim();
            String pass = new String(passField.getPassword());
            if (!uname.isEmpty() && !pass.isEmpty()) {
                try {
                    PreparedStatement ps = this.getConn().prepareStatement("SELECT UserID,PasswordHash,Salt,Role,FullName FROM SystemUser WHERE Username=?");

                    label68: {
                        label74: {
                            try {
                                ps.setString(1, uname);
                                ResultSet rs = ps.executeQuery();
                                if (!rs.next()) {
                                    msgLbl.setText("Invalid username or password.");
                                    break label74;
                                }

                                String storedHash = rs.getString("PasswordHash");
                                String salt = rs.getString("Salt");
                                if (!this.verifyPassword(pass, salt, storedHash)) {
                                    msgLbl.setText("Invalid username or password.");
                                    break label68;
                                }

                                currentUserID = rs.getInt("UserID");
                                currentUsername = uname;
                                currentRole = lab4.Role.valueOf(rs.getString("Role"));
                                this.executeUpdate("UPDATE SystemUser SET LastLogin=NOW() WHERE UserID=?", currentUserID);
                                this.audit("LOGIN", "SystemUser", String.valueOf(currentUserID), "User logged in");
                                dlg.dispose();
                                this.buildUI();
                                this.setVisible(true);
                            } catch (Throwable var13) {
                                Throwable t$ = var13;
                                if (ps != null) {
                                    try {
                                        ps.close();
                                    } catch (Throwable var12) {
                                        Throwable x2 = var12;
                                        t$.addSuppressed(x2);
                                    }
                                }

                                throw new RuntimeException(t$);
                            }

                            if (ps != null) {
                                ps.close();
                            }

                            return;
                        }

                        if (ps != null) {
                            ps.close();
                        }

                        return;
                    }

                    if (ps != null) {
                        ps.close();
                    }

                } catch (Exception var14) {
                    Exception exx = var14;
                    msgLbl.setText("Error: " + exx.getMessage());
                }
            } else {
                msgLbl.setText("Please enter username and password.");
            }
        };
        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);
        registerBtn.addActionListener((e) -> {
            dlg.dispose();
            this.showRegisterDialog();
        });
        dlg.add(panel);
        dlg.setVisible(true);
        if (currentUserID == -1) {
            System.exit(0);
        }

    }

    private void showRegisterDialog() {
        JDialog dlg = new JDialog((Frame)null, "Create Account", true);
        dlg.setSize(440, 460);
        dlg.setLocationRelativeTo((Component)null);
        dlg.setDefaultCloseOperation(2);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SECONDARY);
        panel.setBorder(new EmptyBorder(20, 32, 20, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.fill = 2;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        JLabel titleLbl = new JLabel("Create New Account", 0);
        titleLbl.setFont(new Font("Segoe UI", 1, 16));
        titleLbl.setForeground(PRIMARY);
        panel.add(titleLbl, gc);
        gc.gridwidth = 1;
        String[] labels = new String[]{"Full Name:", "Email:", "Username:", "Password:", "Confirm Password:"};
        JTextField[] fields = new JTextField[5];

        for(int i = 0; i < labels.length; ++i) {
            gc.gridy = i + 1;
            gc.gridx = 0;
            gc.weightx = 0.0;
            panel.add(new JLabel(labels[i]), gc);
            gc.gridx = 1;
            gc.weightx = 1.0;
            fields[i] = (JTextField)(i >= 3 ? new JPasswordField(18) : new JTextField(18));
            panel.add(fields[i], gc);
        }

        gc.gridy = 6;
        gc.gridx = 0;
        gc.weightx = 0.0;
        panel.add(new JLabel("Role:"), gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        JComboBox<String> roleBox = new JComboBox(currentRole == lab4.Role.ADMIN ? new String[]{"STUDENT", "TEACHER", "ADMIN"} : new String[]{"STUDENT", "TEACHER"});
        panel.add(roleBox, gc);
        gc.gridy = 7;
        gc.gridx = 0;
        gc.gridwidth = 2;
        JLabel msgLbl = new JLabel(" ", 0);
        msgLbl.setForeground(ACCENT);
        panel.add(msgLbl, gc);
        gc.gridy = 8;
        JPanel btnRow = new JPanel(new FlowLayout(1, 12, 0));
        btnRow.setBackground(SECONDARY);
        JButton regBtn = this.btn("Register", SUCCESS);
        JButton backBtn = this.btn("Back to Login", Color.GRAY);
        btnRow.add(regBtn);
        btnRow.add(backBtn);
        panel.add(btnRow, gc);
        regBtn.addActionListener((e) -> {
            String fullName = fields[0].getText().trim();
            String email = fields[1].getText().trim();
            String username = fields[2].getText().trim();
            String pass = new String(((JPasswordField)fields[3]).getPassword());
            String confirm = new String(((JPasswordField)fields[4]).getPassword());
            String role = (String)roleBox.getSelectedItem();
            if (!fullName.isEmpty() && !username.isEmpty() && !pass.isEmpty()) {
                if (!pass.equals(confirm)) {
                    msgLbl.setText("Passwords do not match.");
                } else if (pass.length() < 6) {
                    msgLbl.setText("Password must be at least 6 characters.");
                } else if (!this.isValidEmail(email) && !email.isEmpty()) {
                    msgLbl.setText("Invalid email format.");
                } else {
                    Object exists = this.scalarQuery("SELECT COUNT(*) FROM SystemUser WHERE Username=?", username);
                    if (exists != null && ((Number)exists).intValue() > 0) {
                        msgLbl.setText("Username already taken.");
                    } else {
                        String[] saltAndHash = this.hashPassword(pass);
                        if (this.executeUpdate("INSERT INTO SystemUser(Username,PasswordHash,Salt,Role,FullName,Email) VALUES(?,?,?,?,?,?)", username, saltAndHash[1], saltAndHash[0], role, fullName, email)) {
                            
                            JOptionPane.showMessageDialog(dlg, "Account created successfully!\nYou can now log in.");
                            dlg.dispose();
                            this.showLoginDialog();
                        }

                    }
                }
            } else {
                msgLbl.setText("Full Name, Username, and Password are required.");
            }
        });
        backBtn.addActionListener((e) -> {
            dlg.dispose();
            this.showLoginDialog();
        });
        dlg.add(panel);
        dlg.setVisible(true);
    }

    private void buildUI() {
        this.getContentPane().removeAll();
        this.setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setPreferredSize(new Dimension(0, 60));
        JLabel title = new JLabel("   School Records & Repository Management System", 2);
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", 1, 20));
        header.add(title, "Center");
        JPanel rightHeader = new JPanel(new FlowLayout(2, 10, 15));
        rightHeader.setBackground(PRIMARY);
        this.sessionLabel = new JLabel(currentUsername + "  [" + String.valueOf(currentRole) + "]  Connected", 4);
        this.sessionLabel.setForeground(new Color(100, 255, 150));
        this.sessionLabel.setFont(new Font("Segoe UI", 1, 12));
        rightHeader.add(this.sessionLabel);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(ACCENT);
        logoutBtn.setForeground(WHITE);
        logoutBtn.setFont(new Font("Segoe UI", 1, 11));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(12));
        logoutBtn.addActionListener((e) -> {
            this.doLogout();
        });
        rightHeader.add(logoutBtn);
        header.add(rightHeader, "East");
        this.tabs = new JTabbedPane(1);
        this.tabs.setFont(new Font("Segoe UI", 1, 12));
        this.tabs.addTab(" Dashboard", this.buildDashboard());
        this.tabs.addTab(" Principal", this.buildPrincipalPanel());
        this.tabs.addTab(" School Year", this.buildSchoolYearPanel());
        this.tabs.addTab(" Teacher", this.buildTeacherPanel());
        this.tabs.addTab(" Subject", this.buildSubjectPanel());
        this.tabs.addTab(" Student", this.buildStudentPanel());
        this.tabs.addTab(" Section", this.buildSectionPanel());
        this.tabs.addTab(" Admin", this.buildAdminPanel());
        this.tabs.addTab(" Academic Record", this.buildAcademicRecordPanel());
        this.tabs.addTab(" Grades", this.buildGradesPanel());
        this.tabs.addTab(" Reports", this.buildReportsPanel());
        this.tabs.addTab(" JOIN Queries", this.buildJoinPanel());
        this.tabs.addTab(" Advanced Search", this.buildAdvancedSearchPanel());
        this.tabs.addTab(" Data Export", this.buildDataExportPanel());
        this.tabs.addTab(" Audit Log", this.buildAuditLogPanel());
        this.applyRoleRestrictions();
        if (currentRole == lab4.Role.ADMIN) {
            this.tabs.addTab("User Management", this.buildUserManagementPanel());
        }

        this.add(header, "North");
        this.add(this.tabs, "Center");
        this.revalidate();
        this.repaint();
    }

    private void applyRoleRestrictions() {
        if (currentRole == lab4.Role.STUDENT) {
            int[] restricted = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
            int[] var2 = restricted;
            int var3 = restricted.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                int i = var2[var4];
                if (i < this.tabs.getTabCount()) {
                    this.tabs.setEnabledAt(i, false);
                    this.tabs.setToolTipTextAt(i, "Read-only access – contact an Admin");
                }
            }
        } else if (currentRole == lab4.Role.TEACHER && 7 < this.tabs.getTabCount()) {
            this.tabs.setEnabledAt(7, false);
            this.tabs.setToolTipTextAt(7, "Admin-only tab");
        }

    }

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", 0);
        if (confirm == 0) {
            this.audit("LOGOUT", "SystemUser", String.valueOf(currentUserID), "User logged out");
            currentUserID = -1;
            currentUsername = "";
            currentRole = null;
            this.setVisible(false);
            this.showLoginDialog();
        }
    }

    private boolean canWrite() {
        if (currentRole != lab4.Role.ADMIN && currentRole != lab4.Role.TEACHER) {
            JOptionPane.showMessageDialog((Component)null, "Access Denied: Your role (" + String.valueOf(currentRole) + ") is read-only.\nContact an Admin.", "Permission Denied", 2);
            return false;
        } else {
            return true;
        }
    }

    private boolean isAdmin() {
        if (currentRole == lab4.Role.ADMIN) {
            return true;
        } else {
            JOptionPane.showMessageDialog((Component)null, "Access Denied: Admin role required.", "Permission Denied", 2);
            return false;
        }
    }

    private JPanel buildUserManagementPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JLabel hdr = new JLabel("User Management", 2);
        hdr.setFont(new Font("Segoe UI", 1, 16));
        hdr.setForeground(PRIMARY);
        root.add(hdr, "North");
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        root.add(scroll, "Center");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Edit User"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] selID = new JTextField[]{new JTextField()};
        JTextField uname = this.addRow(form, "Username *", g, 0);
        JTextField fullname = this.addRow(form, "Full Name", g, 1);
        JTextField email = this.addRow(form, "Email", g, 2);
        JComboBox<String> roleBox = this.addCombo(form, "Role", new String[]{"STUDENT", "TEACHER", "ADMIN"}, g, 3);
        JTextField newPass = this.addRow(form, "New Password (leave blank to keep)", g, 4);
        g.gridx = 0;
        g.gridy = 5;
        form.add(new JLabel("UserID:"), g);
        g.gridx = 1;
        selID[0].setEditable(false);
        selID[0].setBackground(new Color(230, 230, 230));
        form.add(selID[0], g);
        this.loadTable(table, "SELECT UserID,Username,FullName,Email,Role,CreatedAt,LastLogin FROM SystemUser");
        JPanel btnBar = new JPanel(new FlowLayout(0, 6, 4));
        btnBar.setBackground(SECONDARY);
        JButton btnRefresh = this.btn("Refresh", PRIMARY);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnRefresh.addActionListener((e) -> {
            this.loadTable(table, "SELECT UserID,Username,FullName,Email,Role,CreatedAt,LastLogin FROM SystemUser");
        });
        btnUpdate.addActionListener((e) -> {
            if (selID[0].getText().isEmpty()) {
                JOptionPane.showMessageDialog(root, "Select a user.");
            } else {
                String passVal = newPass.getText().trim();
                if (!passVal.isEmpty()) {
                    String[] sh = this.hashPassword(passVal);
                    this.executeUpdate("UPDATE SystemUser SET FullName=?,Email=?,Role=?,PasswordHash=?,Salt=? WHERE UserID=?", fullname.getText(), email.getText(), roleBox.getSelectedItem(), sh[1], sh[0], selID[0].getText());
                } else {
                    this.executeUpdate("UPDATE SystemUser SET FullName=?,Email=?,Role=? WHERE UserID=?", fullname.getText(), email.getText(), roleBox.getSelectedItem(), selID[0].getText());
                }

                this.audit("UPDATE", "SystemUser", selID[0].getText(), "Admin updated user " + uname.getText());
                JOptionPane.showMessageDialog(root, "User updated.");
                this.loadTable(table, "SELECT UserID,Username,FullName,Email,Role,CreatedAt,LastLogin FROM SystemUser");
            }
        });
        btnDelete.addActionListener((e) -> {
            if (selID[0].getText().isEmpty()) {
                JOptionPane.showMessageDialog(root, "Select a user.");
            } else if (selID[0].getText().equals(String.valueOf(currentUserID))) {
                JOptionPane.showMessageDialog(root, "You cannot delete your own account.");
            } else {
                if (JOptionPane.showConfirmDialog(root, "Delete user " + uname.getText() + "?", "Confirm", 0) == 0) {
                    this.executeUpdate("DELETE FROM SystemUser WHERE UserID=?", selID[0].getText());
                    this.audit("DELETE", "SystemUser", selID[0].getText(), "Admin deleted user " + uname.getText());
                    this.loadTable(table, "SELECT UserID,Username,FullName,Email,Role,CreatedAt,LastLogin FROM SystemUser");
                    this.clearFields(uname, fullname, email, newPass);
                    selID[0].setText("");
                }

            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(uname, fullname, email, newPass);
            selID[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                selID[0].setText(this.str(m.getValueAt(row, 0)));
                uname.setText(this.str(m.getValueAt(row, 1)));
                fullname.setText(this.str(m.getValueAt(row, 2)));
                email.setText(this.str(m.getValueAt(row, 3)));
                roleBox.setSelectedItem(this.str(m.getValueAt(row, 4)));
                newPass.setText("");
            }

        });
        btnBar.add(btnRefresh);
        btnBar.add(btnUpdate);
        btnBar.add(btnDelete);
        btnBar.add(btnClear);
        JPanel south = new JPanel(new BorderLayout());
        south.add(form, "North");
        south.add(btnBar, "South");
        root.add(south, "South");
        this.styleTable(table);
        return root;
    }

    private JPanel buildAuditLogPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JLabel hdr = new JLabel("Audit Log – Activity History", 2);
        hdr.setFont(new Font("Segoe UI", 1, 16));
        hdr.setForeground(PRIMARY);
        root.add(hdr, "North");
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        root.add(scroll, "Center");
        JPanel filterBar = new JPanel(new FlowLayout(0, 8, 6));
        filterBar.setBackground(SECONDARY);
        filterBar.add(new JLabel("User:"));
        JTextField userFilter = new JTextField(12);
        filterBar.add(userFilter);
        filterBar.add(new JLabel("Action:"));
        JComboBox<String> actionFilter = new JComboBox(new String[]{"ALL", "LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE"});
        filterBar.add(actionFilter);
        filterBar.add(new JLabel("Table:"));
        JTextField tableFilter = new JTextField(10);
        filterBar.add(tableFilter);
        JButton btnFilter = this.btn("Filter", PRIMARY);
        JButton btnRefresh = this.btn("Refresh All", SUCCESS);
        filterBar.add(btnFilter);
        filterBar.add(btnRefresh);
        btnRefresh.addActionListener((e) -> {
            this.loadTable(table, "SELECT LogID,Username,Action,TableName,RecordID,Details,ActionTime FROM AuditLog ORDER BY ActionTime DESC LIMIT 500");
        });
        btnFilter.addActionListener((e) -> {
            String uname = userFilter.getText().trim();
            String action = (String)actionFilter.getSelectedItem();
            String tbl = tableFilter.getText().trim();
            StringBuilder sql = new StringBuilder("SELECT LogID,Username,Action,TableName,RecordID,Details,ActionTime FROM AuditLog WHERE 1=1");
            List<Object> params = new ArrayList();
            if (!uname.isEmpty()) {
                sql.append(" AND Username LIKE ?");
                params.add("%" + uname + "%");
            }

            if (!"ALL".equals(action)) {
                sql.append(" AND Action=?");
                params.add(action);
            }

            if (!tbl.isEmpty()) {
                sql.append(" AND TableName LIKE ?");
                params.add("%" + tbl + "%");
            }

            sql.append(" ORDER BY ActionTime DESC LIMIT 500");
            this.loadTable(table, sql.toString(), params.toArray());
        });
        this.loadTable(table, "SELECT LogID,Username,Action,TableName,RecordID,Details,ActionTime FROM AuditLog ORDER BY ActionTime DESC LIMIT 500");
        root.add(filterBar, "South");
        return root;
    }

    private JPanel buildAdvancedSearchPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JLabel hdr = new JLabel("Advanced Search with Filters", 2);
        hdr.setFont(new Font("Segoe UI", 1, 16));
        hdr.setForeground(PRIMARY);
        root.add(hdr, "North");
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(WHITE);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Search Criteria (leave blank to skip)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.fill = 2;
        JTextField studentName = this.addRow(filterPanel, "Student Name (first/last):", g, 0);
        JTextField lrn = this.addRow(filterPanel, "LRN:", g, 1);
        JTextField teacherName = this.addRow(filterPanel, "Teacher Name (first/last):", g, 2);
        JTextField subjectName = this.addRow(filterPanel, "Subject Name/Code:", g, 3);
        JTextField schoolYear = this.addRow(filterPanel, "School Year (e.g. 2024-2025):", g, 4);
        JComboBox<String> sexFilter = this.addCombo(filterPanel, "Student Sex:", new String[]{"Any", "Male", "Female"}, g, 5);
        JComboBox<String> enrollStatus = this.addCombo(filterPanel, "Enrollment Status:", new String[]{"Any", "Active", "Inactive", "Transferred"}, g, 6);
        JComboBox<String> gradeRemark = this.addCombo(filterPanel, "Grade Remarks:", new String[]{"Any", "Passed", "Failed", "Incomplete"}, g, 7);
        JTextField minAvg = this.addRow(filterPanel, "Min Final Average (0-100):", g, 8);
        JTextField maxAvg = this.addRow(filterPanel, "Max Final Average (0-100):", g, 9);
        JTable resultTable = new JTable();
        JScrollPane scroll = new JScrollPane(resultTable);
        JLabel countLbl = new JLabel("Results: 0 records");
        countLbl.setFont(new Font("Segoe UI", 1, 12));
        countLbl.setForeground(PRIMARY);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.setPreferredSize(new Dimension(120, 32));
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnClear.setPreferredSize(new Dimension(100, 32));
        btnClear.addActionListener((e) -> {
            this.clearFields(studentName, lrn, teacherName, subjectName, schoolYear, minAvg, maxAvg);
            sexFilter.setSelectedIndex(0);
            enrollStatus.setSelectedIndex(0);
            gradeRemark.setSelectedIndex(0);
            resultTable.setModel(new DefaultTableModel());
            countLbl.setText("Results: 0 records");
        });
        btnSearch.addActionListener((e) -> {
            StringBuilder sql = new StringBuilder("SELECT DISTINCT s.StudentID, s.LRN,   CONCAT(s.StudentFirstName,' ',s.StudentLastName) AS StudentName,   s.Sex, s.Age, s.EnrollmentStatus,   CONCAT(t.FirstName,' ',t.LastName) AS TeacherName,   sub.SubjectCode, sub.SubjectName,   ar.SchoolYear, ar.Semester,   g.Quarter1, g.Quarter2, g.Quarter3, g.Quarter4,   g.FinalAverage, g.Remarks FROM Student s LEFT JOIN Teacher t       ON s.TeacherID = t.TeacherID LEFT JOIN Subject sub     ON s.SubjectID = sub.SubjectID LEFT JOIN AcademicRecord ar ON ar.StudentID = s.StudentID LEFT JOIN Grades g        ON g.AcademicRecordID = ar.AcademicRecordID WHERE 1=1");
            List<Object> params = new ArrayList();
            String sn = studentName.getText().trim();
            if (!sn.isEmpty()) {
                sql.append(" AND (s.StudentFirstName LIKE ? OR s.StudentLastName LIKE ?)");
                params.add("%" + sn + "%");
                params.add("%" + sn + "%");
            }

            String lrnVal = lrn.getText().trim();
            if (!lrnVal.isEmpty()) {
                sql.append(" AND s.LRN LIKE ?");
                params.add("%" + lrnVal + "%");
            }

            String tn = teacherName.getText().trim();
            if (!tn.isEmpty()) {
                sql.append(" AND (t.FirstName LIKE ? OR t.LastName LIKE ?)");
                params.add("%" + tn + "%");
                params.add("%" + tn + "%");
            }

            String subVal = subjectName.getText().trim();
            if (!subVal.isEmpty()) {
                sql.append(" AND (sub.SubjectName LIKE ? OR sub.SubjectCode LIKE ?)");
                params.add("%" + subVal + "%");
                params.add("%" + subVal + "%");
            }

            String syVal = schoolYear.getText().trim();
            if (!syVal.isEmpty()) {
                sql.append(" AND ar.SchoolYear LIKE ?");
                params.add("%" + syVal + "%");
            }

            String sex = (String)sexFilter.getSelectedItem();
            if (!"Any".equals(sex)) {
                sql.append(" AND s.Sex=?");
                params.add(sex);
            }

            String enroll = (String)enrollStatus.getSelectedItem();
            if (!"Any".equals(enroll)) {
                sql.append(" AND s.EnrollmentStatus=?");
                params.add(enroll);
            }

            String remark = (String)gradeRemark.getSelectedItem();
            if (!"Any".equals(remark)) {
                sql.append(" AND g.Remarks=?");
                params.add(remark);
            }

            String minA = minAvg.getText().trim();
            if (!minA.isEmpty() && this.isValidGrade(minA)) {
                sql.append(" AND g.FinalAverage >= ?");
                params.add(Double.parseDouble(minA));
            }

            String maxA = maxAvg.getText().trim();
            if (!maxA.isEmpty() && this.isValidGrade(maxA)) {
                sql.append(" AND g.FinalAverage <= ?");
                params.add(Double.parseDouble(maxA));
            }

            sql.append(" ORDER BY s.StudentLastName, s.StudentFirstName LIMIT 500");
            this.loadTable(resultTable, sql.toString(), params.toArray());
            int rows = resultTable.getRowCount();
            countLbl.setText("Results: " + rows + " record" + (rows != 1 ? "s" : ""));
            this.audit("CREATE", "AdvancedSearch", "-", "Search executed, " + rows + " results");
        });
        JScrollPane filterScroll = new JScrollPane(filterPanel);
        filterScroll.setPreferredSize(new Dimension(380, 0));
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.setBackground(SECONDARY);
        JPanel btnBar = new JPanel(new FlowLayout(0, 8, 4));
        btnBar.setBackground(SECONDARY);
        btnBar.add(btnSearch);
        btnBar.add(btnClear);
        btnBar.add(countLbl);
        rightPanel.add(btnBar, "North");
        rightPanel.add(scroll, "Center");
        JSplitPane split = new JSplitPane(1, filterScroll, rightPanel);
        split.setDividerLocation(390);
        root.add(split, "Center");
        return root;
    }

    private JPanel buildDataExportPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JLabel hdr = new JLabel("Data Export – CSV & JSON", 2);
        hdr.setFont(new Font("Segoe UI", 1, 16));
        hdr.setForeground(PRIMARY);
        root.add(hdr, "North");
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(WHITE);
        center.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.fill = 2;
        g.gridwidth = 1;
        JTable previewTable = new JTable();
        JScrollPane previewScroll = new JScrollPane(previewTable);
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.0;
        center.add(new JLabel("Dataset to Export:"), g);
        g.gridx = 1;
        g.weightx = 1.0;
        String[] datasets = new String[]{"Students", "Teachers", "Subjects", "Sections", "Academic Records", "Grades", "Full Transcript", "Audit Log"};
        JComboBox<String> datasetBox = new JComboBox(datasets);
        center.add(datasetBox, g);
        g.gridx = 0;
        g.gridy = 1;
        g.gridwidth = 2;
        JLabel statusLbl = new JLabel("Select a dataset and preview, then export.", 0);
        statusLbl.setForeground(Color.GRAY);
        center.add(statusLbl, g);
        g.gridy = 2;
        g.gridwidth = 1;
        g.gridx = 0;
        JButton btnPreview = this.btn("Preview Data", PRIMARY);
        center.add(btnPreview, g);
        g.gridx = 1;
        JPanel exportBtns = new JPanel(new FlowLayout(0, 6, 0));
        exportBtns.setBackground(WHITE);
        JButton btnCSV = this.btn("Export CSV", SUCCESS);
        JButton btnJSON = this.btn("Export JSON", GOLD);
        btnJSON.setForeground(new Color(60, 40, 0));
        exportBtns.add(btnCSV);
        exportBtns.add(btnJSON);
        center.add(exportBtns, g);
        Map<String, String> sqlMap = new LinkedHashMap();
        sqlMap.put("Students", "SELECT StudentID,LRN,StudentFirstName,StudentMiddleName,StudentLastName,Age,Sex,Birthdate,EnrollmentStatus,AddressBarangay,AddressMunicipality,AddressProvince FROM Student");
        sqlMap.put("Teachers", "SELECT TeacherID,FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Qualification,Email,DateHired,Status FROM Teacher");
        sqlMap.put("Subjects", "SELECT SubjectID,SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,Room,TeacherID FROM Subject");
        sqlMap.put("Sections", "SELECT SectionID,SectionName,GradeLevel,Adviser,SchoolYear FROM Section");
        sqlMap.put("Academic Records", "SELECT AcademicRecordID,StudentID,TeacherID,SubjectID,SubjectCode,SubjectName,Semester,SchoolYear FROM AcademicRecord");
        sqlMap.put("Grades", "SELECT GradeID,AcademicRecordID,Quarter1,Quarter2,Quarter3,Quarter4,FinalAverage,Remarks FROM Grades");
        sqlMap.put("Full Transcript", "SELECT CONCAT(s.StudentFirstName,' ',s.StudentLastName) AS Student,ar.SubjectName,ar.Semester,ar.SchoolYear,g.Quarter1,g.Quarter2,g.Quarter3,g.Quarter4,g.FinalAverage,g.Remarks,CONCAT(t.FirstName,' ',t.LastName) AS Teacher FROM AcademicRecord ar JOIN Student s ON ar.StudentID=s.StudentID LEFT JOIN Grades g ON ar.AcademicRecordID=g.AcademicRecordID LEFT JOIN Teacher t ON ar.TeacherID=t.TeacherID");
        sqlMap.put("Audit Log", "SELECT LogID,Username,Action,TableName,RecordID,Details,ActionTime FROM AuditLog ORDER BY ActionTime DESC");
        btnPreview.addActionListener((e) -> {
            String selected = (String)datasetBox.getSelectedItem();
            String sql = (String)sqlMap.get(selected);
            if (sql != null) {
                this.loadTable(previewTable, sql);
                int var10001 = previewTable.getRowCount();
                statusLbl.setText("Preview: " + var10001 + " rows  |  " + selected);
                statusLbl.setForeground(PRIMARY);
            }

        });
        btnCSV.addActionListener((e) -> {
            String selected = (String)datasetBox.getSelectedItem();
            String sql = (String)sqlMap.get(selected);
            if (sql != null) {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File(selected.replace(" ", "_") + ".csv"));
                fc.setFileFilter(new FileNameExtensionFilter("CSV files", new String[]{"csv"}));
                if (fc.showSaveDialog(root) == 0) {
                    File file = fc.getSelectedFile();
                    if (!file.getName().endsWith(".csv")) {
                        file = new File(file.getAbsolutePath() + ".csv");
                    }

                    try {
                        this.exportCSV(sql, file);
                        this.audit("CREATE", "Export", selected, "CSV exported to " + file.getName());
                        statusLbl.setText("CSV exported: " + file.getAbsolutePath());
                        statusLbl.setForeground(SUCCESS);
                        JOptionPane.showMessageDialog(root, "CSV exported successfully!\n" + file.getAbsolutePath());
                    } catch (Exception var11) {
                        Exception ex = var11;
                        JOptionPane.showMessageDialog(root, "Export failed:\n" + ex.getMessage());
                    }
                }

            }
        });
        btnJSON.addActionListener((e) -> {
            String selected = (String)datasetBox.getSelectedItem();
            String sql = (String)sqlMap.get(selected);
            if (sql != null) {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File(selected.replace(" ", "_") + ".json"));
                fc.setFileFilter(new FileNameExtensionFilter("JSON files", new String[]{"json"}));
                if (fc.showSaveDialog(root) == 0) {
                    File file = fc.getSelectedFile();
                    if (!file.getName().endsWith(".json")) {
                        file = new File(file.getAbsolutePath() + ".json");
                    }

                    try {
                        this.exportJSON(sql, file);
                        this.audit("CREATE", "Export", selected, "JSON exported to " + file.getName());
                        statusLbl.setText("JSON exported: " + file.getAbsolutePath());
                        statusLbl.setForeground(SUCCESS);
                        JOptionPane.showMessageDialog(root, "JSON exported successfully!\n" + file.getAbsolutePath());
                    } catch (Exception var11) {
                        Exception ex = var11;
                        JOptionPane.showMessageDialog(root, "Export failed:\n" + ex.getMessage());
                    }
                }

            }
        });
        root.add(center, "North");
        root.add(previewScroll, "Center");
        return root;
    }

    private void exportCSV(String sql, File file) throws Exception {
        PreparedStatement ps = this.getConn().prepareStatement(sql);

        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            try {
                ResultSet rs = ps.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                StringBuilder header = new StringBuilder();

                for(int i = 1; i <= cols; ++i) {
                    if (i > 1) {
                        header.append(",");
                    }

                    header.append(this.csvEscape(meta.getColumnLabel(i)));
                }

                pw.println(header);

                while(rs.next()) {
                    StringBuilder row = new StringBuilder();

                    for(int i = 1; i <= cols; ++i) {
                        if (i > 1) {
                            row.append(",");
                        }

                        Object val = rs.getObject(i);
                        row.append(val == null ? "" : this.csvEscape(val.toString()));
                    }

                    pw.println(row);
                }
            } catch (Throwable var14) {
                try {
                    pw.close();
                } catch (Throwable var13) {
                    var14.addSuppressed(var13);
                }

                throw var14;
            }

            pw.close();
        } catch (Throwable var15) {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable var12) {
                    var15.addSuppressed(var12);
                }
            }

            throw var15;
        }

        if (ps != null) {
            ps.close();
        }

    }

    private String csvEscape(String val) {
        return !val.contains(",") && !val.contains("\"") && !val.contains("\n") ? val : "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private void exportJSON(String sql, File file) throws Exception {
        PreparedStatement ps = this.getConn().prepareStatement(sql);

        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            try {
                ResultSet rs = ps.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                pw.println("[");
                boolean firstRow = true;

                while(true) {
                    if (!rs.next()) {
                        pw.println();
                        pw.println("]");
                        break;
                    }

                    if (!firstRow) {
                        pw.println(",");
                    }

                    firstRow = false;
                    pw.print("  {");

                    for(int i = 1; i <= cols; ++i) {
                        if (i > 1) {
                            pw.print(", ");
                        }

                        String colName = meta.getColumnLabel(i);
                        Object val = rs.getObject(i);
                        String var10001 = this.jsonEscape(colName);
                        pw.print("\"" + var10001 + "\": ");
                        if (val == null) {
                            pw.print("null");
                        } else if (val instanceof Number) {
                            pw.print(val);
                        } else {
                            var10001 = this.jsonEscape(val.toString());
                            pw.print("\"" + var10001 + "\"");
                        }
                    }

                    pw.print("}");
                }
            } catch (Throwable var14) {
                try {
                    pw.close();
                } catch (Throwable var13) {
                    var14.addSuppressed(var13);
                }

                throw var14;
            }

            pw.close();
        } catch (Throwable var15) {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable var12) {
                    var15.addSuppressed(var12);
                }
            }

            throw var15;
        }

        if (ps != null) {
            ps.close();
        }

    }

    private String jsonEscape(String val) {
        return val.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(WHITE);
        b.setFont(new Font("Segoe UI", 1, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(12));
        b.setPreferredSize(new Dimension(110, 32));
        return b;
    }

    private JTextField addRow(JPanel form, String label, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", 0, 12));
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", 0, 12));
        form.add(tf, gbc);
        return tf;
    }

    private JComboBox<String> addCombo(JPanel form, String label, String[] items, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", 0, 12));
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<String> cb = new JComboBox(items);
        cb.setFont(new Font("Segoe UI", 0, 12));
        form.add(cb, gbc);
        return cb;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(24);
        t.setFont(new Font("Segoe UI", 0, 12));
        t.setSelectionBackground(new Color(173, 216, 230));
        t.setGridColor(new Color(220, 220, 220));
        JTableHeader h = t.getTableHeader();
        h.setBackground(TABLE_HDR);
        h.setForeground(WHITE);
        h.setFont(new Font("Segoe UI", 1, 12));
    }

    private void loadTable(JTable table, String sql, Object... params) {
        try {
            PreparedStatement ps = this.getConn().prepareStatement(sql);

            try {
                for(int i = 0; i < params.length; ++i) {
                    ps.setObject(i + 1, params[i]);
                }

                ResultSet rs = ps.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                String[] colNames = new String[cols];

                for(int i = 1; i <= cols; ++i) {
                    colNames[i - 1] = meta.getColumnLabel(i);
                }

                DefaultTableModel model = new DefaultTableModel(colNames, 0) {
                    {
                        Objects.requireNonNull(lab4.this);
                    }

                    public boolean isCellEditable(int r, int c) {
                        return false;
                    }
                };

                while(true) {
                    if (!rs.next()) {
                        table.setModel(model);
                        this.styleTable(table);
                        break;
                    }

                    Object[] row = new Object[cols];

                    for(int i = 1; i <= cols; ++i) {
                        row[i - 1] = rs.getObject(i);
                    }

                    model.addRow(row);
                }
            } catch (Throwable var13) {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                    }
                }

                throw var13;
            }

            if (ps != null) {
                ps.close();
            }
        } catch (Exception var14) {
            Exception e = var14;
            JOptionPane.showMessageDialog((Component)null, "Load error: " + e.getMessage());
        }

    }

    private boolean executeUpdate(String sql, Object... params) {
        try {
            PreparedStatement ps = this.getConn().prepareStatement(sql);

            boolean var12;
            try {
                int i = 0;

                while(true) {
                    if (i >= params.length) {
                        ps.executeUpdate();
                        var12 = true;
                        break;
                    }

                    ps.setObject(i + 1, params[i]);
                    ++i;
                }
            } catch (Throwable var7) {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (ps != null) {
                ps.close();
            }

            return var12;
        } catch (SQLIntegrityConstraintViolationException var8) {
            JOptionPane.showMessageDialog((Component)null, "Duplicate or constraint error:\n" + var8.getMessage(), "Constraint Error", 0);
        } catch (Exception var9) {
            Exception e = var9;
            JOptionPane.showMessageDialog((Component)null, "DB Error:\n" + e.getMessage(), "Error", 0);
        }

        return false;
    }

    private Object scalarQuery(String sql, Object... params) {
        try {
            PreparedStatement ps = this.getConn().prepareStatement(sql);

            Object var5;
            label60: {
                try {
                    for(int i = 0; i < params.length; ++i) {
                        ps.setObject(i + 1, params[i]);
                    }

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        var5 = rs.getObject(1);
                        break label60;
                    }
                } catch (Throwable var7) {
                    if (ps != null) {
                        try {
                            ps.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (ps != null) {
                    ps.close();
                }

                return null;
            }

            if (ps != null) {
                ps.close();
            }

            return var5;
        } catch (Exception var8) {
            return null;
        }
    }

    private boolean isValidName(String s) {
        return s != null && s.matches("[A-Za-z\\s\\-]+");
    }

    private boolean isValidEmail(String e) {
        return e == null || e.isEmpty() || e.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidContact(String c) {
        return c == null || c.isEmpty() || c.matches("\\d{7,15}");
    }

    private boolean isValidDate(String d) {
        if (d != null && !d.isEmpty()) {
            try {
                (new SimpleDateFormat("yyyy-MM-dd")).parse(d);
                return true;
            } catch (Exception var3) {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean isValidGrade(String g) {
        if (g != null && !g.isEmpty()) {
            try {
                double v = Double.parseDouble(g);
                return v >= 0.0 && v <= 100.0;
            } catch (Exception var4) {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean existsInDB(String table, String col, Object val) {
        Object r = this.scalarQuery("SELECT COUNT(*) FROM " + table + " WHERE " + col + "=?", val);
        return r != null && ((Number)r).intValue() > 0;
    }

    private String calculateAge(String birthdateStr) {
        if (birthdateStr != null && !birthdateStr.trim().isEmpty()) {
            try {
                LocalDate birthdate = LocalDate.parse(birthdateStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate today = LocalDate.now();
                return birthdate.isAfter(today) ? "" : String.valueOf(Period.between(birthdate, today).getYears());
            } catch (DateTimeParseException var4) {
                return "";
            }
        } else {
            return "";
        }
    }

    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        p.setBackground(SECONDARY);
        JLabel lbl = new JLabel("Welcome, " + currentUsername + "  [" + String.valueOf(currentRole) + "]  –  School Records & Repository Management System", 0);
        lbl.setFont(new Font("Segoe UI", 1, 16));
        lbl.setForeground(PRIMARY);
        p.add(lbl, "North");
        JPanel grid = new JPanel(new GridLayout(3, 3, 12, 12));
        grid.setBackground(SECONDARY);
        String[][] cards = new String[][]{{"Students", "SELECT COUNT(*) FROM Student"}, {"Teachers", "SELECT COUNT(*) FROM Teacher WHERE Status='Active'"}, {"Subjects", "SELECT COUNT(*) FROM Subject"}, {"Sections", "SELECT COUNT(*) FROM Section"}, {"Academic Records", "SELECT COUNT(*) FROM AcademicRecord"}, {"Grades Issued", "SELECT COUNT(*) FROM Grades"}, {"School Years", "SELECT COUNT(*) FROM SchoolYear"}, {"Principals", "SELECT COUNT(*) FROM Principal"}, {"System Users", "SELECT COUNT(*) FROM SystemUser"}};
        String[][] var5 = cards;
        int var6 = cards.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String[] c = var5[var7];
            Object val = this.scalarQuery(c[1]);
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(PRIMARY, 1, true), new EmptyBorder(14, 14, 14, 14)));
            JLabel num = new JLabel(val == null ? "0" : val.toString(), 0);
            num.setFont(new Font("Segoe UI", 1, 36));
            num.setForeground(PRIMARY);
            JLabel name = new JLabel(c[0], 0);
            name.setFont(new Font("Segoe UI", 0, 13));
            card.add(num, "Center");
            card.add(name, "South");
            grid.add(card);
        }

        p.add(grid, "Center");
        JButton refresh = this.btn("Refresh", PRIMARY);
        refresh.addActionListener((e) -> {
            this.refreshDashboard(grid, cards);
        });
        JPanel bot = new JPanel(new FlowLayout(2));
        bot.setBackground(SECONDARY);
        bot.add(refresh);
        p.add(bot, "South");
        return p;
    }

    private void refreshDashboard(JPanel grid, String[][] cards) {
        Component[] comps = grid.getComponents();

        for(int i = 0; i < comps.length && i < cards.length; ++i) {
            JPanel card = (JPanel)comps[i];
            Object val = this.scalarQuery(cards[i][1]);
            ((JLabel)card.getComponent(0)).setText(val == null ? "0" : val.toString());
        }

        grid.repaint();
    }

    private JPanel buildPrincipalPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Principal Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField fName = this.addRow(form, "First Name *", g, 0);
        JTextField mName = this.addRow(form, "Middle Name", g, 1);
        JTextField lName = this.addRow(form, "Last Name *", g, 2);
        JComboBox<String> gender = this.addCombo(form, "Gender", new String[]{"", "Male", "Female"}, g, 3);
        JTextField contact = this.addRow(form, "Contact Number", g, 4);
        JTextField email = this.addRow(form, "Email", g, 5);
        JTextField dateApp = this.addRow(form, "Date Appointed (yyyy-MM-dd)", g, 6);
        g.gridx = 0;
        g.gridy = 7;
        form.add(new JLabel("Principal ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT * FROM Principal");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT * FROM Principal WHERE FirstName LIKE ? OR LastName LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (this.isValidName(fName.getText()) && this.isValidName(lName.getText())) {
                    if (!this.isValidContact(contact.getText())) {
                        JOptionPane.showMessageDialog(root, "Invalid contact.");
                    } else if (!this.isValidEmail(email.getText())) {
                        JOptionPane.showMessageDialog(root, "Invalid email.");
                    } else if (!this.isValidDate(dateApp.getText())) {
                        JOptionPane.showMessageDialog(root, "Invalid date.");
                    } else {
                        if (this.executeUpdate("INSERT INTO Principal(FirstName,MiddleName,LastName,Gender,ContactNumber,Email,DateAppointed) VALUES(?,?,?,?,?,?,?)", fName.getText(), mName.getText(), lName.getText(), gender.getSelectedItem(), contact.getText(), email.getText(), dateApp.getText().isEmpty() ? null : dateApp.getText())) {
                            String var10004 = fName.getText();
                            this.audit("CREATE", "Principal", "new", var10004 + " " + lName.getText());
                            JOptionPane.showMessageDialog(root, "Principal added!");
                            this.loadTable(table, "SELECT * FROM Principal");
                            this.clearFields(fName, mName, lName, contact, email, dateApp);
                        }

                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Names must contain letters only.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else if (this.isValidName(fName.getText()) && this.isValidName(lName.getText())) {
                    if (this.executeUpdate("UPDATE Principal SET FirstName=?,MiddleName=?,LastName=?,Gender=?,ContactNumber=?,Email=?,DateAppointed=? WHERE PrincipalID=?", fName.getText(), mName.getText(), lName.getText(), gender.getSelectedItem(), contact.getText(), email.getText(), dateApp.getText().isEmpty() ? null : dateApp.getText(), id[0].getText())) {
                        String var10003 = id[0].getText();
                        String var10004 = fName.getText();
                        this.audit("UPDATE", "Principal", var10003, var10004 + " " + lName.getText());
                        JOptionPane.showMessageDialog(root, "Principal updated!");
                        this.loadTable(table, "SELECT * FROM Principal");
                    }

                } else {
                    JOptionPane.showMessageDialog(root, "Invalid name.");
                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete this principal?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Principal", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Principal WHERE PrincipalID=?", id[0].getText());
                        this.loadTable(table, "SELECT * FROM Principal");
                        this.clearFields(fName, mName, lName, contact, email, dateApp);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(fName, mName, lName, contact, email, dateApp);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((e) -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                fName.setText(this.str(m.getValueAt(row, 1)));
                mName.setText(this.str(m.getValueAt(row, 2)));
                lName.setText(this.str(m.getValueAt(row, 3)));
                gender.setSelectedItem(this.str(m.getValueAt(row, 4)));
                contact.setText(this.str(m.getValueAt(row, 5)));
                email.setText(this.str(m.getValueAt(row, 6)));
                dateApp.setText(this.str(m.getValueAt(row, 7)));
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildSchoolYearPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("School Year Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField yearLabel = this.addRow(form, "Year Label (e.g. 2025-2026) *", g, 0);
        JTextField startDate = this.addRow(form, "Start Date (yyyy-MM-dd) *", g, 1);
        JTextField endDate = this.addRow(form, "End Date (yyyy-MM-dd) *", g, 2);
        JComboBox<String> status = this.addCombo(form, "Status", new String[]{"Upcoming", "Active", "Completed"}, g, 3);
        g.gridx = 0;
        g.gridy = 4;
        form.add(new JLabel("SchoolYear ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT * FROM SchoolYear");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT * FROM SchoolYear WHERE YearLabel LIKE ?", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (!yearLabel.getText().matches("\\d{4}-\\d{4}")) {
                    JOptionPane.showMessageDialog(root, "Year Label must be YYYY-YYYY.");
                } else if (this.isValidDate(startDate.getText()) && this.isValidDate(endDate.getText())) {
                    if (this.executeUpdate("INSERT INTO SchoolYear(YearLabel,StartDate,EndDate,Status) VALUES(?,?,?,?)", yearLabel.getText(), startDate.getText(), endDate.getText(), status.getSelectedItem())) {
                        this.audit("CREATE", "SchoolYear", "new", yearLabel.getText());
                        JOptionPane.showMessageDialog(root, "School Year added!");
                        this.loadTable(table, "SELECT * FROM SchoolYear");
                        this.clearFields(yearLabel, startDate, endDate);
                    }

                } else {
                    JOptionPane.showMessageDialog(root, "Invalid date format.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (this.executeUpdate("UPDATE SchoolYear SET YearLabel=?,StartDate=?,EndDate=?,Status=? WHERE SchoolYearID=?", yearLabel.getText(), startDate.getText(), endDate.getText(), status.getSelectedItem(), id[0].getText())) {
                        this.audit("UPDATE", "SchoolYear", id[0].getText(), yearLabel.getText());
                        JOptionPane.showMessageDialog(root, "School Year updated!");
                        this.loadTable(table, "SELECT * FROM SchoolYear");
                    }

                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "SchoolYear", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM SchoolYear WHERE SchoolYearID=?", id[0].getText());
                        this.loadTable(table, "SELECT * FROM SchoolYear");
                        this.clearFields(yearLabel, startDate, endDate);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(yearLabel, startDate, endDate);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((e) -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                yearLabel.setText(this.str(m.getValueAt(row, 1)));
                startDate.setText(this.str(m.getValueAt(row, 2)));
                endDate.setText(this.str(m.getValueAt(row, 3)));
                status.setSelectedItem(this.str(m.getValueAt(row, 4)));
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildTeacherPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Teacher Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField fName = this.addRow(form, "First Name *", g, 0);
        JTextField mName = this.addRow(form, "Middle Name", g, 1);
        JTextField lName = this.addRow(form, "Last Name *", g, 2);
        JComboBox<String> sex = this.addCombo(form, "Sex", new String[]{"", "Male", "Female"}, g, 3);
        JTextField contact = this.addRow(form, "Contact Number", g, 4);
        JTextField dept = this.addRow(form, "Department", g, 5);
        JTextField qual = this.addRow(form, "Qualification", g, 6);
        JTextField brgy = this.addRow(form, "Address Barangay", g, 7);
        JTextField mun = this.addRow(form, "Address Municipality", g, 8);
        JTextField prov = this.addRow(form, "Address Province", g, 9);
        JTextField postal = this.addRow(form, "Postal Code", g, 10);
        JTextField email = this.addRow(form, "Email", g, 11);
        JTextField dateH = this.addRow(form, "Date Hired (yyyy-MM-dd)", g, 12);
        JComboBox<String> status = this.addCombo(form, "Status", new String[]{"Active", "OnLeave", "Resigned"}, g, 13);
        JTextField advisory = this.addRow(form, "Advisory Section", g, 14);
        g.gridx = 0;
        g.gridy = 15;
        form.add(new JLabel("Teacher ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT TeacherID,FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Status FROM Teacher");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT TeacherID,FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Status FROM Teacher WHERE FirstName LIKE ? OR LastName LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (this.isValidName(fName.getText()) && this.isValidName(lName.getText())) {
                    if (this.isValidContact(contact.getText()) && this.isValidEmail(email.getText())) {
                        if (!this.isValidDate(dateH.getText())) {
                            JOptionPane.showMessageDialog(root, "Invalid date.");
                        } else {
                            if (this.executeUpdate("INSERT INTO Teacher(FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Qualification,AddressBarangay,AddressMunicipality,AddressProvince,AddressPostalCode,Email,DateHired,Status,AdvisorySection) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", fName.getText(), mName.getText(), lName.getText(), sex.getSelectedItem(), contact.getText(), dept.getText(), qual.getText(), brgy.getText(), mun.getText(), prov.getText(), postal.getText(), email.getText(), dateH.getText().isEmpty() ? null : dateH.getText(), status.getSelectedItem(), advisory.getText())) {
                                String var10004 = fName.getText();
                                this.audit("CREATE", "Teacher", "new", var10004 + " " + lName.getText());
                                JOptionPane.showMessageDialog(root, "Teacher added!");
                                this.loadTable(table, "SELECT TeacherID,FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Status FROM Teacher");
                                this.clearFields(fName, mName, lName, contact, dept, qual, brgy, mun, prov, postal, email, dateH, advisory);
                            }

                        }
                    } else {
                        JOptionPane.showMessageDialog(root, "Invalid contact/email.");
                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Invalid name format.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else if (this.isValidName(fName.getText()) && this.isValidName(lName.getText())) {
                    if (this.executeUpdate("UPDATE Teacher SET FirstName=?,MiddleName=?,LastName=?,Sex=?,ContactNumber=?,Department=?,Qualification=?,AddressBarangay=?,AddressMunicipality=?,AddressProvince=?,AddressPostalCode=?,Email=?,DateHired=?,Status=?,AdvisorySection=? WHERE TeacherID=?", fName.getText(), mName.getText(), lName.getText(), sex.getSelectedItem(), contact.getText(), dept.getText(), qual.getText(), brgy.getText(), mun.getText(), prov.getText(), postal.getText(), email.getText(), dateH.getText().isEmpty() ? null : dateH.getText(), status.getSelectedItem(), advisory.getText(), id[0].getText())) {
                        String var10003 = id[0].getText();
                        String var10004 = fName.getText();
                        this.audit("UPDATE", "Teacher", var10003, var10004 + " " + lName.getText());
                        JOptionPane.showMessageDialog(root, "Teacher updated!");
                        this.loadTable(table, "SELECT TeacherID,FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Status FROM Teacher");
                    }

                } else {
                    JOptionPane.showMessageDialog(root, "Invalid name.");
                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete teacher?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Teacher", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Teacher WHERE TeacherID=?", id[0].getText());
                        this.loadTable(table, "SELECT TeacherID,FirstName,MiddleName,LastName,Sex,ContactNumber,Department,Status FROM Teacher");
                        this.clearFields(fName, mName, lName, contact, dept, qual, brgy, mun, prov, postal, email, dateH, advisory);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(fName, mName, lName, contact, dept, qual, brgy, mun, prov, postal, email, dateH, advisory);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                fName.setText(this.str(m.getValueAt(row, 1)));
                mName.setText(this.str(m.getValueAt(row, 2)));
                lName.setText(this.str(m.getValueAt(row, 3)));
                sex.setSelectedItem(this.str(m.getValueAt(row, 4)));
                contact.setText(this.str(m.getValueAt(row, 5)));
                dept.setText(this.str(m.getValueAt(row, 6)));
                status.setSelectedItem(this.str(m.getValueAt(row, 7)));

                try {
                    PreparedStatement ps = this.getConn().prepareStatement("SELECT * FROM Teacher WHERE TeacherID=?");

                    try {
                        ps.setInt(1, Integer.parseInt(id[0].getText()));
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            qual.setText(rs.getString("Qualification"));
                            brgy.setText(rs.getString("AddressBarangay"));
                            mun.setText(rs.getString("AddressMunicipality"));
                            prov.setText(rs.getString("AddressProvince"));
                            postal.setText(rs.getString("AddressPostalCode"));
                            email.setText(rs.getString("Email"));
                            Object dh = rs.getObject("DateHired");
                            dateH.setText(dh == null ? "" : dh.toString());
                            advisory.setText(rs.getString("AdvisorySection"));
                        }
                    } catch (Throwable var25) {
                        Throwable t$ = var25;
                        if (ps != null) {
                            try {
                                ps.close();
                            } catch (Throwable var24) {
                                Throwable x2 = var24;
                                t$.addSuppressed(x2);
                            }
                        }

                        throw new RuntimeException(t$);
                    }

                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception var26) {
                    Exception ex = var26;
                    ex.printStackTrace();
                }
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildSubjectPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Subject Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField code = this.addRow(form, "Subject Code * (unique)", g, 0);
        JTextField name = this.addRow(form, "Subject Name *", g, 1);
        JTextField gradeL = this.addRow(form, "Grade Level", g, 2);
        JTextField strand = this.addRow(form, "Strand", g, 3);
        JComboBox<String> sem = this.addCombo(form, "Semester", new String[]{"", "1st", "2nd"}, g, 4);
        JTextField dept = this.addRow(form, "Department", g, 5);
        JTextField sched = this.addRow(form, "Class Schedule", g, 6);
        JTextField room = this.addRow(form, "Room", g, 7);
        JTextField tID = this.addRow(form, "Teacher ID (FK)", g, 8);
        g.gridx = 0;
        g.gridy = 9;
        form.add(new JLabel("Subject ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT SubjectID,SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,TeacherID FROM Subject");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT SubjectID,SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,TeacherID FROM Subject WHERE SubjectCode LIKE ? OR SubjectName LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (!code.getText().isEmpty() && !name.getText().isEmpty()) {
                    if (!tID.getText().isEmpty() && !this.existsInDB("Teacher", "TeacherID", tID.getText())) {
                        JOptionPane.showMessageDialog(root, "TeacherID not found.");
                    } else {
                        if (this.executeUpdate("INSERT INTO Subject(SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,ClassSchedule,Room,TeacherID) VALUES(?,?,?,?,?,?,?,?,?)", code.getText(), name.getText(), gradeL.getText(), strand.getText(), sem.getSelectedItem().toString().isEmpty() ? null : sem.getSelectedItem(), dept.getText(), sched.getText(), room.getText(), tID.getText().isEmpty() ? null : Integer.parseInt(tID.getText()))) {
                            String var10004 = code.getText();
                            this.audit("CREATE", "Subject", "new", var10004 + " " + name.getText());
                            JOptionPane.showMessageDialog(root, "Subject added!");
                            this.loadTable(table, "SELECT SubjectID,SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,TeacherID FROM Subject");
                            this.clearFields(code, name, gradeL, strand, dept, sched, room, tID);
                        }

                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Code and Name required.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else if (!tID.getText().isEmpty() && !this.existsInDB("Teacher", "TeacherID", tID.getText())) {
                    JOptionPane.showMessageDialog(root, "TeacherID not found.");
                } else {
                    if (this.executeUpdate("UPDATE Subject SET SubjectCode=?,SubjectName=?,GradeLevel=?,Strand=?,Semester=?,Department=?,ClassSchedule=?,Room=?,TeacherID=? WHERE SubjectID=?", code.getText(), name.getText(), gradeL.getText(), strand.getText(), sem.getSelectedItem().toString().isEmpty() ? null : sem.getSelectedItem(), dept.getText(), sched.getText(), room.getText(), tID.getText().isEmpty() ? null : Integer.parseInt(tID.getText()), id[0].getText())) {
                        this.audit("UPDATE", "Subject", id[0].getText(), code.getText());
                        JOptionPane.showMessageDialog(root, "Subject updated!");
                        this.loadTable(table, "SELECT SubjectID,SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,TeacherID FROM Subject");
                    }

                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete subject?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Subject", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Subject WHERE SubjectID=?", id[0].getText());
                        this.loadTable(table, "SELECT SubjectID,SubjectCode,SubjectName,GradeLevel,Strand,Semester,Department,TeacherID FROM Subject");
                        this.clearFields(code, name, gradeL, strand, dept, sched, room, tID);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(code, name, gradeL, strand, dept, sched, room, tID);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                code.setText(this.str(m.getValueAt(row, 1)));
                name.setText(this.str(m.getValueAt(row, 2)));
                gradeL.setText(this.str(m.getValueAt(row, 3)));
                strand.setText(this.str(m.getValueAt(row, 4)));
                sem.setSelectedItem(this.str(m.getValueAt(row, 5)));
                dept.setText(this.str(m.getValueAt(row, 6)));
                tID.setText(this.str(m.getValueAt(row, 7)));

                try {
                    PreparedStatement ps = this.getConn().prepareStatement("SELECT * FROM Subject WHERE SubjectID=?");

                    try {
                        ps.setInt(1, Integer.parseInt(id[0].getText()));
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            sched.setText(rs.getString("ClassSchedule"));
                            room.setText(rs.getString("Room"));
                        }
                    } catch (Throwable var19) {
                        Throwable t$ = var19;
                        if (ps != null) {
                            try {
                                ps.close();
                            } catch (Throwable var18) {
                                Throwable x2 = var18;
                                t$.addSuppressed(x2);
                            }
                        }

                        throw new RuntimeException(t$);
                    }

                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception var20) {
                    Exception ex = var20;
                    ex.printStackTrace();
                }
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildStudentPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Student Complete Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField lrn = this.addRow(form, "LRN * (unique)", g, 0);
        JTextField sfName = this.addRow(form, "Student First Name *", g, 1);
        JTextField smName = this.addRow(form, "Student Middle Name", g, 2);
        JTextField slName = this.addRow(form, "Student Last Name *", g, 3);
        final JTextField bdate = this.addRow(form, "Birthdate (yyyy-MM-dd)", g, 4);
        g.gridx = 0;
        g.gridy = 5;
        g.weightx = 0.0;
        JLabel ageLabel = new JLabel("Age (auto-calculated)");
        ageLabel.setFont(new Font("Segoe UI", 0, 12));
        form.add(ageLabel, g);
        g.gridx = 1;
        g.weightx = 1.0;
        final JTextField age = new JTextField();
        age.setFont(new Font("Segoe UI", 0, 12));
        age.setEditable(false);
        age.setBackground(new Color(230, 230, 230));
        form.add(age, g);
        bdate.addFocusListener(new FocusAdapter() {
            {
                Objects.requireNonNull(lab4.this);
            }

            public void focusLost(FocusEvent e) {
                String c = lab4.this.calculateAge(bdate.getText());
                age.setText(c);
                if (!c.isEmpty()) {
                    age.setForeground(lab4.PRIMARY);
                }

            }
        });
        bdate.addActionListener((e) -> {
            age.setText(this.calculateAge(bdate.getText()));
        });
        JComboBox<String> sex = this.addCombo(form, "Sex", new String[]{"", "Male", "Female"}, g, 6);
        JTextField religion = this.addRow(form, "Religion", g, 7);
        JTextField gfName = this.addRow(form, "Guardian First Name *", g, 8);
        JTextField gmName = this.addRow(form, "Guardian Middle Name *", g, 9);
        JTextField glName = this.addRow(form, "Guardian Last Name *", g, 10);
        JTextField pobBrgy = this.addRow(form, "Birth Place Barangay", g, 11);
        JTextField pobMun = this.addRow(form, "Birth Place Municipality", g, 12);
        JTextField pobProv = this.addRow(form, "Birth Place Province", g, 13);
        JTextField pobPost = this.addRow(form, "Birth Place Postal", g, 14);
        JTextField adBrgy = this.addRow(form, "Address Barangay", g, 15);
        JTextField adMun = this.addRow(form, "Address Municipality", g, 16);
        JTextField adProv = this.addRow(form, "Address Province", g, 17);
        JTextField adPost = this.addRow(form, "Address Postal Code", g, 18);
        JComboBox<String> enroll = this.addCombo(form, "Enrollment Status", new String[]{"Active", "Inactive", "Transferred"}, g, 19);
        JTextField subID = this.addRow(form, "Subject ID (FK)", g, 20);
        JTextField tID = this.addRow(form, "Teacher ID (FK)", g, 21);
        g.gridx = 0;
        g.gridy = 22;
        form.add(new JLabel("Student ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT StudentID,LRN,StudentFirstName,StudentLastName,Age,Sex,EnrollmentStatus FROM Student");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT StudentID,LRN,StudentFirstName,StudentLastName,Age,Sex,EnrollmentStatus FROM Student WHERE StudentFirstName LIKE ? OR StudentLastName LIKE ? OR LRN LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (this.isValidName(sfName.getText()) && this.isValidName(slName.getText())) {
                    if (this.isValidName(gfName.getText()) && this.isValidName(glName.getText())) {
                        if (!this.isValidDate(bdate.getText())) {
                            JOptionPane.showMessageDialog(root, "Invalid birthdate.");
                        } else {
                            String computedAge = this.calculateAge(bdate.getText());
                            age.setText(computedAge);
                            if (!subID.getText().isEmpty() && !this.existsInDB("Subject", "SubjectID", subID.getText())) {
                                JOptionPane.showMessageDialog(root, "SubjectID not found.");
                            } else if (!tID.getText().isEmpty() && !this.existsInDB("Teacher", "TeacherID", tID.getText())) {
                                JOptionPane.showMessageDialog(root, "TeacherID not found.");
                            } else {
                                if (this.executeUpdate("INSERT INTO Student(LRN,StudentFirstName,StudentMiddleName,StudentLastName,GuardianFirstName,GuardianMiddleName,GuardianLastName,Age,Sex,Birthdate,PlaceOfBirthBarangay,PlaceOfBirthMunicipality,PlaceOfBirthProvince,PlaceOfBirthPostalCode,Religion,AddressBarangay,AddressMunicipality,AddressProvince,AddressPostalCode,EnrollmentStatus,SubjectID,TeacherID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", lrn.getText(), sfName.getText(), smName.getText(), slName.getText(), gfName.getText(), gmName.getText(), glName.getText(), computedAge.isEmpty() ? null : Integer.parseInt(computedAge), sex.getSelectedItem(), bdate.getText().isEmpty() ? null : bdate.getText(), pobBrgy.getText(), pobMun.getText(), pobProv.getText(), pobPost.getText(), religion.getText(), adBrgy.getText(), adMun.getText(), adProv.getText(), adPost.getText(), enroll.getSelectedItem(), subID.getText().isEmpty() ? null : Integer.parseInt(subID.getText()), tID.getText().isEmpty() ? null : Integer.parseInt(tID.getText()))) {
                                    String var10004 = sfName.getText();
                                    this.audit("CREATE", "Student", "new", var10004 + " " + slName.getText());
                                    JOptionPane.showMessageDialog(root, "Student added!");
                                    this.loadTable(table, "SELECT StudentID,LRN,StudentFirstName,StudentLastName,Age,Sex,EnrollmentStatus FROM Student");
                                    this.clearStudentForm(lrn, sfName, smName, slName, gfName, gmName, glName, bdate, religion, pobBrgy, pobMun, pobProv, pobPost, adBrgy, adMun, adProv, adPost, subID, tID);
                                    age.setText("");
                                }

                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(root, "Invalid guardian name.");
                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Invalid student name.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else if (this.isValidName(sfName.getText()) && this.isValidName(slName.getText())) {
                    if (!this.isValidDate(bdate.getText())) {
                        JOptionPane.showMessageDialog(root, "Invalid date.");
                    } else {
                        String computedAge = this.calculateAge(bdate.getText());
                        age.setText(computedAge);
                        if (!subID.getText().isEmpty() && !this.existsInDB("Subject", "SubjectID", subID.getText())) {
                            JOptionPane.showMessageDialog(root, "SubjectID not found.");
                        } else if (!tID.getText().isEmpty() && !this.existsInDB("Teacher", "TeacherID", tID.getText())) {
                            JOptionPane.showMessageDialog(root, "TeacherID not found.");
                        } else {
                            if (this.executeUpdate("UPDATE Student SET LRN=?,StudentFirstName=?,StudentMiddleName=?,StudentLastName=?,GuardianFirstName=?,GuardianMiddleName=?,GuardianLastName=?,Age=?,Sex=?,Birthdate=?,PlaceOfBirthBarangay=?,PlaceOfBirthMunicipality=?,PlaceOfBirthProvince=?,PlaceOfBirthPostalCode=?,Religion=?,AddressBarangay=?,AddressMunicipality=?,AddressProvince=?,AddressPostalCode=?,EnrollmentStatus=?,SubjectID=?,TeacherID=? WHERE StudentID=?", lrn.getText(), sfName.getText(), smName.getText(), slName.getText(), gfName.getText(), gmName.getText(), glName.getText(), computedAge.isEmpty() ? null : Integer.parseInt(computedAge), sex.getSelectedItem(), bdate.getText().isEmpty() ? null : bdate.getText(), pobBrgy.getText(), pobMun.getText(), pobProv.getText(), pobPost.getText(), religion.getText(), adBrgy.getText(), adMun.getText(), adProv.getText(), adPost.getText(), enroll.getSelectedItem(), subID.getText().isEmpty() ? null : Integer.parseInt(subID.getText()), tID.getText().isEmpty() ? null : Integer.parseInt(tID.getText()), id[0].getText())) {
                                String var10003 = id[0].getText();
                                String var10004 = sfName.getText();
                                this.audit("UPDATE", "Student", var10003, var10004 + " " + slName.getText());
                                JOptionPane.showMessageDialog(root, "Student updated!");
                                this.loadTable(table, "SELECT StudentID,LRN,StudentFirstName,StudentLastName,Age,Sex,EnrollmentStatus FROM Student");
                            }

                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Invalid name.");
                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete student?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Student", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Student WHERE StudentID=?", id[0].getText());
                        this.loadTable(table, "SELECT StudentID,LRN,StudentFirstName,StudentLastName,Age,Sex,EnrollmentStatus FROM Student");
                        this.clearStudentForm(lrn, sfName, smName, slName, gfName, gmName, glName, bdate, religion, pobBrgy, pobMun, pobProv, pobPost, adBrgy, adMun, adProv, adPost, subID, tID);
                        age.setText("");
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearStudentForm(lrn, sfName, smName, slName, gfName, gmName, glName, bdate, religion, pobBrgy, pobMun, pobProv, pobPost, adBrgy, adMun, adProv, adPost, subID, tID);
            age.setText("");
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));

                try {
                    PreparedStatement ps = this.getConn().prepareStatement("SELECT * FROM Student WHERE StudentID=?");

                    try {
                        ps.setInt(1, Integer.parseInt(id[0].getText()));
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            lrn.setText(rs.getString("LRN"));
                            sfName.setText(rs.getString("StudentFirstName"));
                            smName.setText(rs.getString("StudentMiddleName"));
                            slName.setText(rs.getString("StudentLastName"));
                            gfName.setText(rs.getString("GuardianFirstName"));
                            gmName.setText(rs.getString("GuardianMiddleName"));
                            glName.setText(rs.getString("GuardianLastName"));
                            sex.setSelectedItem(rs.getString("Sex"));
                            Object bd = rs.getObject("Birthdate");
                            String bdStr = bd == null ? "" : bd.toString();
                            bdate.setText(bdStr);
                            age.setText(this.calculateAge(bdStr));
                            religion.setText(rs.getString("Religion"));
                            pobBrgy.setText(rs.getString("PlaceOfBirthBarangay"));
                            pobMun.setText(rs.getString("PlaceOfBirthMunicipality"));
                            pobProv.setText(rs.getString("PlaceOfBirthProvince"));
                            pobPost.setText(rs.getString("PlaceOfBirthPostalCode"));
                            adBrgy.setText(rs.getString("AddressBarangay"));
                            adMun.setText(rs.getString("AddressMunicipality"));
                            adProv.setText(rs.getString("AddressProvince"));
                            adPost.setText(rs.getString("AddressPostalCode"));
                            enroll.setSelectedItem(rs.getString("EnrollmentStatus"));
                            Object si = rs.getObject("SubjectID");
                            subID.setText(si == null ? "" : si.toString());
                            Object ti = rs.getObject("TeacherID");
                            tID.setText(ti == null ? "" : ti.toString());
                        }
                    } catch (Throwable var35) {
                        Throwable t$ = var35;
                        if (ps != null) {
                            try {
                                ps.close();
                            } catch (Throwable var34) {
                                Throwable x2 = var34;
                                t$.addSuppressed(x2);
                            }
                        }

                        throw new RuntimeException(t$);
                    }

                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception var36) {
                    Exception ex = var36;
                    ex.printStackTrace();
                }
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private void clearStudentForm(JTextField... fields) {
        JTextField[] var2 = fields;
        int var3 = fields.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            JTextField f = var2[var4];
            f.setText("");
        }

    }

    private JPanel buildSectionPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Section Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField secName = this.addRow(form, "Section Name *", g, 0);
        JTextField gradeL = this.addRow(form, "Grade Level *", g, 1);
        JTextField adviser = this.addRow(form, "Adviser *", g, 2);
        JTextField syLabel = this.addRow(form, "School Year", g, 3);
        JTextField syID = this.addRow(form, "SchoolYear ID (FK)", g, 4);
        g.gridx = 0;
        g.gridy = 5;
        form.add(new JLabel("Section ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT * FROM Section");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT * FROM Section WHERE SectionName LIKE ? OR GradeLevel LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (!secName.getText().isEmpty() && !gradeL.getText().isEmpty() && !adviser.getText().isEmpty()) {
                    if (!syID.getText().isEmpty() && !this.existsInDB("SchoolYear", "SchoolYearID", syID.getText())) {
                        JOptionPane.showMessageDialog(root, "SchoolYearID not found.");
                    } else {
                        if (this.executeUpdate("INSERT INTO Section(SectionName,GradeLevel,Adviser,SchoolYear,SchoolYearID) VALUES(?,?,?,?,?)", secName.getText(), gradeL.getText(), adviser.getText(), syLabel.getText(), syID.getText().isEmpty() ? null : Integer.parseInt(syID.getText()))) {
                            this.audit("CREATE", "Section", "new", secName.getText());
                            JOptionPane.showMessageDialog(root, "Section added!");
                            this.loadTable(table, "SELECT * FROM Section");
                            this.clearFields(secName, gradeL, adviser, syLabel, syID);
                        }

                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Name, Grade, and Adviser required.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (this.executeUpdate("UPDATE Section SET SectionName=?,GradeLevel=?,Adviser=?,SchoolYear=?,SchoolYearID=? WHERE SectionID=?", secName.getText(), gradeL.getText(), adviser.getText(), syLabel.getText(), syID.getText().isEmpty() ? null : Integer.parseInt(syID.getText()), id[0].getText())) {
                        this.audit("UPDATE", "Section", id[0].getText(), secName.getText());
                        JOptionPane.showMessageDialog(root, "Section updated!");
                        this.loadTable(table, "SELECT * FROM Section");
                    }

                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete section?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Section", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Section WHERE SectionID=?", id[0].getText());
                        this.loadTable(table, "SELECT * FROM Section");
                        this.clearFields(secName, gradeL, adviser, syLabel, syID);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(secName, gradeL, adviser, syLabel, syID);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                secName.setText(this.str(m.getValueAt(row, 1)));
                gradeL.setText(this.str(m.getValueAt(row, 2)));
                adviser.setText(this.str(m.getValueAt(row, 3)));
                syLabel.setText(this.str(m.getValueAt(row, 4)));
                syID.setText(this.str(m.getValueAt(row, 5)));
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildAdminPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Admin Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField adminName = this.addRow(form, "Admin Name", g, 0);
        JTextField fName = this.addRow(form, "First Name *", g, 1);
        JTextField mName = this.addRow(form, "Middle Name", g, 2);
        JTextField lName = this.addRow(form, "Last Name *", g, 3);
        JTextField contact = this.addRow(form, "Contact Number", g, 4);
        JTextField email = this.addRow(form, "Email Address", g, 5);
        JComboBox<String> sex = this.addCombo(form, "Sex", new String[]{"", "Male", "Female"}, g, 6);
        JTextField position = this.addRow(form, "Position", g, 7);
        JTextField dept = this.addRow(form, "Department", g, 8);
        JTextField brgy = this.addRow(form, "Address Barangay", g, 9);
        JTextField mun = this.addRow(form, "Address Municipality", g, 10);
        JTextField prov = this.addRow(form, "Address Province", g, 11);
        JTextField postal = this.addRow(form, "Postal Code", g, 12);
        g.gridx = 0;
        g.gridy = 13;
        form.add(new JLabel("Admin ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT AdminID,AdminName,FirstName,LastName,Position,Department FROM Admin");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT AdminID,AdminName,FirstName,LastName,Position,Department FROM Admin WHERE FirstName LIKE ? OR LastName LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (this.isValidName(fName.getText()) && this.isValidName(lName.getText())) {
                    if (this.isValidContact(contact.getText()) && this.isValidEmail(email.getText())) {
                        if (this.executeUpdate("INSERT INTO Admin(AdminName,FirstName,MiddleName,LastName,ContactNumber,EmailAddress,Sex,Position,Department,AddressBarangay,AddressMunicipality,AddressProvince,AddressPostalCode) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", adminName.getText(), fName.getText(), mName.getText(), lName.getText(), contact.getText(), email.getText(), sex.getSelectedItem(), position.getText(), dept.getText(), brgy.getText(), mun.getText(), prov.getText(), postal.getText())) {
                            String var10004 = fName.getText();
                            this.audit("CREATE", "Admin", "new", var10004 + " " + lName.getText());
                            JOptionPane.showMessageDialog(root, "Admin added!");
                            this.loadTable(table, "SELECT AdminID,AdminName,FirstName,LastName,Position,Department FROM Admin");
                            this.clearFields(adminName, fName, mName, lName, contact, email, position, dept, brgy, mun, prov, postal);
                        }

                    } else {
                        JOptionPane.showMessageDialog(root, "Invalid contact/email.");
                    }
                } else {
                    JOptionPane.showMessageDialog(root, "Invalid name.");
                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (this.executeUpdate("UPDATE Admin SET AdminName=?,FirstName=?,MiddleName=?,LastName=?,ContactNumber=?,EmailAddress=?,Sex=?,Position=?,Department=?,AddressBarangay=?,AddressMunicipality=?,AddressProvince=?,AddressPostalCode=? WHERE AdminID=?", adminName.getText(), fName.getText(), mName.getText(), lName.getText(), contact.getText(), email.getText(), sex.getSelectedItem(), position.getText(), dept.getText(), brgy.getText(), mun.getText(), prov.getText(), postal.getText(), id[0].getText())) {
                        String var10003 = id[0].getText();
                        String var10004 = fName.getText();
                        this.audit("UPDATE", "Admin", var10003, var10004 + " " + lName.getText());
                        JOptionPane.showMessageDialog(root, "Admin updated!");
                        this.loadTable(table, "SELECT AdminID,AdminName,FirstName,LastName,Position,Department FROM Admin");
                    }

                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete admin?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Admin", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Admin WHERE AdminID=?", id[0].getText());
                        this.loadTable(table, "SELECT AdminID,AdminName,FirstName,LastName,Position,Department FROM Admin");
                        this.clearFields(adminName, fName, mName, lName, contact, email, position, dept, brgy, mun, prov, postal);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(adminName, fName, mName, lName, contact, email, position, dept, brgy, mun, prov, postal);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                adminName.setText(this.str(m.getValueAt(row, 1)));
                fName.setText(this.str(m.getValueAt(row, 2)));
                lName.setText(this.str(m.getValueAt(row, 3)));
                position.setText(this.str(m.getValueAt(row, 4)));
                dept.setText(this.str(m.getValueAt(row, 5)));

                try {
                    PreparedStatement ps = this.getConn().prepareStatement("SELECT * FROM Admin WHERE AdminID=?");

                    try {
                        ps.setInt(1, Integer.parseInt(id[0].getText()));
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            mName.setText(rs.getString("MiddleName"));
                            contact.setText(rs.getString("ContactNumber"));
                            email.setText(rs.getString("EmailAddress"));
                            sex.setSelectedItem(rs.getString("Sex"));
                            brgy.setText(rs.getString("AddressBarangay"));
                            mun.setText(rs.getString("AddressMunicipality"));
                            prov.setText(rs.getString("AddressProvince"));
                            postal.setText(rs.getString("AddressPostalCode"));
                        }
                    } catch (Throwable var23) {
                        Throwable t$ = var23;
                        if (ps != null) {
                            try {
                                ps.close();
                            } catch (Throwable var22) {
                                Throwable x2 = var22;
                                t$.addSuppressed(x2);
                            }
                        }

                        throw new RuntimeException(t$);
                    }

                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception var24) {
                    Exception ex = var24;
                    ex.printStackTrace();
                }
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildAcademicRecordPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Academic Record Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField studentID = this.addRow(form, "Student ID (FK) *", g, 0);
        JTextField teacherID = this.addRow(form, "Teacher ID (FK)", g, 1);
        JTextField subjectID = this.addRow(form, "Subject ID (FK)", g, 2);
        JTextField subCode = this.addRow(form, "Subject Code", g, 3);
        JTextField subName = this.addRow(form, "Subject Name", g, 4);
        JComboBox<String> sem = this.addCombo(form, "Semester", new String[]{"", "1st", "2nd"}, g, 5);
        JTextField syYear = this.addRow(form, "School Year", g, 6);
        JTextField syID = this.addRow(form, "SchoolYear ID (FK)", g, 7);
        g.gridx = 0;
        g.gridy = 8;
        form.add(new JLabel("AcademicRecord ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT * FROM AcademicRecord");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT * FROM AcademicRecord WHERE SubjectCode LIKE ? OR SubjectName LIKE ? OR SchoolYear LIKE ?", "%" + search.getText() + "%", "%" + search.getText() + "%", "%" + search.getText() + "%");
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (studentID.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Student ID required.");
                } else if (!this.existsInDB("Student", "StudentID", studentID.getText())) {
                    JOptionPane.showMessageDialog(root, "StudentID not found.");
                } else if (!teacherID.getText().isEmpty() && !this.existsInDB("Teacher", "TeacherID", teacherID.getText())) {
                    JOptionPane.showMessageDialog(root, "TeacherID not found.");
                } else if (!subjectID.getText().isEmpty() && !this.existsInDB("Subject", "SubjectID", subjectID.getText())) {
                    JOptionPane.showMessageDialog(root, "SubjectID not found.");
                } else if (!syID.getText().isEmpty() && !this.existsInDB("SchoolYear", "SchoolYearID", syID.getText())) {
                    JOptionPane.showMessageDialog(root, "SchoolYearID not found.");
                } else {
                    if (this.executeUpdate("INSERT INTO AcademicRecord(StudentID,TeacherID,SubjectID,SubjectCode,SubjectName,Semester,SchoolYear,SchoolYearID) VALUES(?,?,?,?,?,?,?,?)", Integer.parseInt(studentID.getText()), teacherID.getText().isEmpty() ? null : Integer.parseInt(teacherID.getText()), subjectID.getText().isEmpty() ? null : Integer.parseInt(subjectID.getText()), subCode.getText(), subName.getText(), sem.getSelectedItem().toString().isEmpty() ? null : sem.getSelectedItem(), syYear.getText(), syID.getText().isEmpty() ? null : Integer.parseInt(syID.getText()))) {
                        this.audit("CREATE", "AcademicRecord", "new", "StudentID=" + studentID.getText());
                        JOptionPane.showMessageDialog(root, "Academic Record added!");
                        this.loadTable(table, "SELECT * FROM AcademicRecord");
                        this.clearFields(studentID, teacherID, subjectID, subCode, subName, syYear, syID);
                    }

                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else if (!this.existsInDB("Student", "StudentID", studentID.getText())) {
                    JOptionPane.showMessageDialog(root, "StudentID not found.");
                } else {
                    if (this.executeUpdate("UPDATE AcademicRecord SET StudentID=?,TeacherID=?,SubjectID=?,SubjectCode=?,SubjectName=?,Semester=?,SchoolYear=?,SchoolYearID=? WHERE AcademicRecordID=?", Integer.parseInt(studentID.getText()), teacherID.getText().isEmpty() ? null : Integer.parseInt(teacherID.getText()), subjectID.getText().isEmpty() ? null : Integer.parseInt(subjectID.getText()), subCode.getText(), subName.getText(), sem.getSelectedItem().toString().isEmpty() ? null : sem.getSelectedItem(), syYear.getText(), syID.getText().isEmpty() ? null : Integer.parseInt(syID.getText()), id[0].getText())) {
                        this.audit("UPDATE", "AcademicRecord", id[0].getText(), "StudentID=" + studentID.getText());
                        JOptionPane.showMessageDialog(root, "Academic Record updated!");
                        this.loadTable(table, "SELECT * FROM AcademicRecord");
                    }

                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete record?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "AcademicRecord", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM AcademicRecord WHERE AcademicRecordID=?", id[0].getText());
                        this.loadTable(table, "SELECT * FROM AcademicRecord");
                        this.clearFields(studentID, teacherID, subjectID, subCode, subName, syYear, syID);
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(studentID, teacherID, subjectID, subCode, subName, syYear, syID);
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                studentID.setText(this.str(m.getValueAt(row, 1)));
                teacherID.setText(this.str(m.getValueAt(row, 2)));
                subjectID.setText(this.str(m.getValueAt(row, 3)));
                subCode.setText(this.str(m.getValueAt(row, 4)));
                subName.setText(this.str(m.getValueAt(row, 5)));
                sem.setSelectedItem(this.str(m.getValueAt(row, 6)));
                syYear.setText(this.str(m.getValueAt(row, 7)));
                syID.setText(this.str(m.getValueAt(row, 8)));
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildGradesPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Grades Information"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill = 2;
        JTextField[] id = new JTextField[]{new JTextField()};
        JTextField arID = this.addRow(form, "AcademicRecord ID (FK) *", g, 0);
        JTextField q1 = this.addRow(form, "Quarter 1 (0-100)", g, 1);
        JTextField q2 = this.addRow(form, "Quarter 2 (0-100)", g, 2);
        JTextField q3 = this.addRow(form, "Quarter 3 (0-100)", g, 3);
        JTextField q4 = this.addRow(form, "Quarter 4 (0-100)", g, 4);
        JTextField avg = this.addRow(form, "Final Average (auto-calc)", g, 5);
        avg.setEditable(false);
        avg.setBackground(new Color(230, 230, 230));
        JComboBox<String> remarks = this.addCombo(form, "Remarks", new String[]{"Incomplete", "Passed", "Failed"}, g, 6);
        final ActionListener calcAvg = (e) -> {
            try {
                double total = 0.0;
                int cnt = 0;
                if (!q1.getText().isEmpty()) {
                    total += Double.parseDouble(q1.getText());
                    ++cnt;
                }

                if (!q2.getText().isEmpty()) {
                    total += Double.parseDouble(q2.getText());
                    ++cnt;
                }

                if (!q3.getText().isEmpty()) {
                    total += Double.parseDouble(q3.getText());
                    ++cnt;
                }

                if (!q4.getText().isEmpty()) {
                    total += Double.parseDouble(q4.getText());
                    ++cnt;
                }

                if (cnt > 0) {
                    double a = total / (double)cnt;
                    avg.setText(String.format("%.2f", a));
                    remarks.setSelectedItem(a >= 75.0 ? "Passed" : "Failed");
                }
            } catch (Exception var12) {
            }

        };
        q1.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                calcAvg.actionPerformed((ActionEvent)null);
            }
        });
        q2.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                calcAvg.actionPerformed((ActionEvent)null);
            }
        });
        q3.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                calcAvg.actionPerformed((ActionEvent)null);
            }
        });
        q4.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                calcAvg.actionPerformed((ActionEvent)null);
            }
        });
        g.gridx = 0;
        g.gridy = 7;
        form.add(new JLabel("Grade ID:"), g);
        g.gridx = 1;
        id[0].setEditable(false);
        id[0].setBackground(new Color(230, 230, 230));
        form.add(id[0], g);
        JTable table = new JTable();
        JScrollPane scroll = new JScrollPane(table);
        this.loadTable(table, "SELECT * FROM Grades");
        JTextField search = new JTextField(20);
        JButton btnSearch = this.btn("Search", PRIMARY);
        btnSearch.addActionListener((e) -> {
            this.loadTable(table, "SELECT * FROM Grades WHERE AcademicRecordID=?", search.getText());
        });
        JButton btnAdd = this.btn("Add", SUCCESS);
        JButton btnUpdate = this.btn("Update", new Color(255, 140, 0));
        JButton btnDelete = this.btn("Delete", ACCENT);
        JButton btnClear = this.btn("Clear", Color.GRAY);
        btnAdd.addActionListener((e) -> {
            if (this.canWrite()) {
                if (arID.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "AcademicRecord ID required.");
                } else if (!this.existsInDB("AcademicRecord", "AcademicRecordID", arID.getText())) {
                    JOptionPane.showMessageDialog(root, "AcademicRecordID not found.");
                } else {
                    JTextField[] arr$ = new JTextField[]{q1, q2, q3, q4};
                    int len$ = arr$.length;

                    for(int i$ = 0; i$ < len$; ++i$) {
                        JTextField qf = arr$[i$];
                        if (!this.isValidGrade(qf.getText())) {
                            JOptionPane.showMessageDialog(root, "Grades must be 0-100.");
                            return;
                        }
                    }

                    if (this.executeUpdate("INSERT INTO Grades(AcademicRecordID,Quarter1,Quarter2,Quarter3,Quarter4,FinalAverage,Remarks) VALUES(?,?,?,?,?,?,?)", Integer.parseInt(arID.getText()), q1.getText().isEmpty() ? null : Double.parseDouble(q1.getText()), q2.getText().isEmpty() ? null : Double.parseDouble(q2.getText()), q3.getText().isEmpty() ? null : Double.parseDouble(q3.getText()), q4.getText().isEmpty() ? null : Double.parseDouble(q4.getText()), avg.getText().isEmpty() ? null : Double.parseDouble(avg.getText()), remarks.getSelectedItem())) {
                        this.audit("CREATE", "Grades", "new", "ARID=" + arID.getText());
                        JOptionPane.showMessageDialog(root, "Grade added!");
                        this.loadTable(table, "SELECT * FROM Grades");
                        this.clearFields(arID, q1, q2, q3, q4);
                        avg.setText("");
                    }

                }
            }
        });
        btnUpdate.addActionListener((e) -> {
            if (this.canWrite()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    JTextField[] arr$ = new JTextField[]{q1, q2, q3, q4};
                    int len$ = arr$.length;

                    for(int i$ = 0; i$ < len$; ++i$) {
                        JTextField qf = arr$[i$];
                        if (!this.isValidGrade(qf.getText())) {
                            JOptionPane.showMessageDialog(root, "Grades must be 0-100.");
                            return;
                        }
                    }

                    if (this.executeUpdate("UPDATE Grades SET AcademicRecordID=?,Quarter1=?,Quarter2=?,Quarter3=?,Quarter4=?,FinalAverage=?,Remarks=? WHERE GradeID=?", Integer.parseInt(arID.getText()), q1.getText().isEmpty() ? null : Double.parseDouble(q1.getText()), q2.getText().isEmpty() ? null : Double.parseDouble(q2.getText()), q3.getText().isEmpty() ? null : Double.parseDouble(q3.getText()), q4.getText().isEmpty() ? null : Double.parseDouble(q4.getText()), avg.getText().isEmpty() ? null : Double.parseDouble(avg.getText()), remarks.getSelectedItem(), id[0].getText())) {
                        this.audit("UPDATE", "Grades", id[0].getText(), "ARID=" + arID.getText());
                        JOptionPane.showMessageDialog(root, "Grade updated!");
                        this.loadTable(table, "SELECT * FROM Grades");
                    }

                }
            }
        });
        btnDelete.addActionListener((e) -> {
            if (this.isAdmin()) {
                if (id[0].getText().isEmpty()) {
                    JOptionPane.showMessageDialog(root, "Select a record.");
                } else {
                    if (JOptionPane.showConfirmDialog(root, "Delete grade?", "Confirm", 0) == 0) {
                        this.audit("DELETE", "Grades", id[0].getText(), "Deleted");
                        this.executeUpdate("DELETE FROM Grades WHERE GradeID=?", id[0].getText());
                        this.loadTable(table, "SELECT * FROM Grades");
                        this.clearFields(arID, q1, q2, q3, q4);
                        avg.setText("");
                        id[0].setText("");
                    }

                }
            }
        });
        btnClear.addActionListener((e) -> {
            this.clearFields(arID, q1, q2, q3, q4);
            avg.setText("");
            id[0].setText("");
        });
        table.getSelectionModel().addListSelectionListener((ev) -> {
            if (!ev.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                DefaultTableModel m = (DefaultTableModel)table.getModel();
                int row = table.getSelectedRow();
                id[0].setText(this.str(m.getValueAt(row, 0)));
                arID.setText(this.str(m.getValueAt(row, 1)));
                q1.setText(this.str(m.getValueAt(row, 2)));
                q2.setText(this.str(m.getValueAt(row, 3)));
                q3.setText(this.str(m.getValueAt(row, 4)));
                q4.setText(this.str(m.getValueAt(row, 5)));
                avg.setText(this.str(m.getValueAt(row, 6)));
                remarks.setSelectedItem(this.str(m.getValueAt(row, 7)));
            }

        });
        return this.assemblePanel(root, form, scroll, search, btnSearch, btnAdd, btnUpdate, btnDelete, btnClear);
    }

    private JPanel buildReportsPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JLabel title = new JLabel("Aggregation Reports", 2);
        title.setFont(new Font("Segoe UI", 1, 16));
        title.setForeground(PRIMARY);
        root.add(title, "North");
        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font("Monospaced", 0, 13));
        JScrollPane scroll = new JScrollPane(output);
        JTable table = new JTable();
        JScrollPane tableScroll = new JScrollPane(table);
        JSplitPane split = new JSplitPane(0, scroll, tableScroll);
        split.setDividerLocation(200);
        root.add(split, "Center");
        JPanel btnPanel = new JPanel(new FlowLayout(0, 8, 8));
        btnPanel.setBackground(SECONDARY);
        String[][] reports = new String[][]{{"Total Students", "SELECT COUNT(*) AS TotalStudents FROM Student"}, {"Active Teachers", "SELECT COUNT(*) AS ActiveTeachers FROM Teacher WHERE Status='Active'"}, {"Total Subjects", "SELECT COUNT(*) AS TotalSubjects FROM Subject"}, {"Avg Grade/Subject", "SELECT ar.SubjectName,AVG(g.FinalAverage) AS AvgGrade FROM Grades g JOIN AcademicRecord ar ON g.AcademicRecordID=ar.AcademicRecordID GROUP BY ar.SubjectName"}, {"Highest Final Avg", "SELECT MAX(FinalAverage) AS HighestAvg FROM Grades"}, {"Lowest Final Avg", "SELECT MIN(FinalAverage) AS LowestAvg FROM Grades"}, {"Students per Subject", "SELECT SubjectID,COUNT(*) AS StudentCount FROM Student GROUP BY SubjectID"}, {"Passed Students", "SELECT COUNT(*) AS PassedCount FROM Grades WHERE Remarks='Passed'"}, {"Failed Students", "SELECT COUNT(*) AS FailedCount FROM Grades WHERE Remarks='Failed'"}, {"Sections per Grade", "SELECT GradeLevel,COUNT(*) AS SectionCount FROM Section GROUP BY GradeLevel"}};
        String[][] var10 = reports;
        int var11 = reports.length;

        for(int var12 = 0; var12 < var11; ++var12) {
            String[] r = var10[var12];
            JButton b = this.btn(r[0], PRIMARY);
            b.setPreferredSize(new Dimension(170, 32));
            b.addActionListener((e) -> {
                this.loadTable(table, r[1]);

                try {
                    PreparedStatement ps = this.getConn().prepareStatement(r[1]);

                    try {
                        ResultSet rs = ps.executeQuery();
                        ResultSetMetaData meta = rs.getMetaData();
                        int cols = meta.getColumnCount();
                        StringBuilder sb = new StringBuilder("=== " + r[0] + " ===\n");
                        int i = 1;

                        while(true) {
                            if (i > cols) {
                                sb.append("\n").append("-".repeat(cols * 25)).append("\n");

                                while(rs.next()) {
                                    for(i = 1; i <= cols; ++i) {
                                        sb.append(String.format("%-25s", rs.getObject(i)));
                                    }

                                    sb.append("\n");
                                }

                                output.setText(sb.toString());
                                break;
                            }

                            sb.append(String.format("%-25s", meta.getColumnLabel(i)));
                            ++i;
                        }
                    } catch (Throwable var12b) {
                        Throwable t$ = var12b;
                        if (ps != null) {
                            try {
                                ps.close();
                            } catch (Throwable var11b) {
                                Throwable x2 = var11b;
                                t$.addSuppressed(x2);
                            }
                        }

                        throw new RuntimeException(t$);
                    }

                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception var13) {
                    Exception exx = var13;
                    output.setText("Error: " + exx.getMessage());
                }

            });
            btnPanel.add(b);
        }

        JScrollPane btnScroll = new JScrollPane(btnPanel, 21, 30);
        btnScroll.setPreferredSize(new Dimension(0, 60));
        root.add(btnScroll, "South");
        return root;
    }

    private JPanel buildJoinPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(SECONDARY);
        JLabel title = new JLabel("JOIN Query Views", 2);
        title.setFont(new Font("Segoe UI", 1, 16));
        title.setForeground(PRIMARY);
        root.add(title, "North");
        JTable table = new JTable();
        root.add(new JScrollPane(table), "Center");
        JPanel btnPanel = new JPanel(new FlowLayout(0, 8, 8));
        btnPanel.setBackground(SECONDARY);
        String[][] joins = new String[][]{{"Student+Subject+Teacher", "SELECT s.StudentID,CONCAT(s.StudentFirstName,' ',s.StudentLastName) AS StudentName,sub.SubjectCode,sub.SubjectName,CONCAT(t.FirstName,' ',t.LastName) AS TeacherName FROM Student s LEFT JOIN Subject sub ON s.SubjectID=sub.SubjectID LEFT JOIN Teacher t ON s.TeacherID=t.TeacherID"}, {"Academic+Grades", "SELECT ar.AcademicRecordID,ar.SubjectCode,ar.SubjectName,ar.SchoolYear,g.Quarter1,g.Quarter2,g.Quarter3,g.Quarter4,g.FinalAverage,g.Remarks FROM AcademicRecord ar LEFT JOIN Grades g ON ar.AcademicRecordID=g.AcademicRecordID"}, {"Section+SchoolYear", "SELECT sec.SectionID,sec.SectionName,sec.GradeLevel,sec.Adviser,sy.YearLabel,sy.Status AS SchoolYearStatus FROM Section sec LEFT JOIN SchoolYear sy ON sec.SchoolYearID=sy.SchoolYearID"}, {"Student+Adviser+Subject", "SELECT s.LRN,CONCAT(s.StudentFirstName,' ',s.StudentLastName) AS Student,CONCAT(t.FirstName,' ',t.LastName) AS Adviser,sub.SubjectCode,sub.SubjectName,s.EnrollmentStatus FROM Student s LEFT JOIN Teacher t ON s.TeacherID=t.TeacherID LEFT JOIN Subject sub ON s.SubjectID=sub.SubjectID"}, {"Full Transcript", "SELECT CONCAT(s.StudentFirstName,' ',s.StudentLastName) AS Student,ar.SubjectName,ar.Semester,ar.SchoolYear,g.Quarter1,g.Quarter2,g.Quarter3,g.Quarter4,g.FinalAverage,g.Remarks,CONCAT(t.FirstName,' ',t.LastName) AS Teacher FROM AcademicRecord ar JOIN Student s ON ar.StudentID=s.StudentID LEFT JOIN Grades g ON ar.AcademicRecordID=g.AcademicRecordID LEFT JOIN Teacher t ON ar.TeacherID=t.TeacherID"}, {"Subject+Teacher Info", "SELECT sub.SubjectCode,sub.SubjectName,sub.GradeLevel,sub.Strand,sub.Semester,CONCAT(t.FirstName,' ',t.LastName) AS Teacher,t.Department,t.Status FROM Subject sub LEFT JOIN Teacher t ON sub.TeacherID=t.TeacherID"}};
        String[][] var6 = joins;
        int var7 = joins.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            String[] j = var6[var8];
            JButton b = this.btn(j[0], PRIMARY);
            b.setPreferredSize(new Dimension(200, 32));
            b.addActionListener((e) -> {
                this.loadTable(table, j[1]);
            });
            btnPanel.add(b);
        }

        JScrollPane btnScroll = new JScrollPane(btnPanel, 21, 30);
        btnScroll.setPreferredSize(new Dimension(0, 60));
        root.add(btnScroll, "South");
        return root;
    }

    private JPanel assemblePanel(JPanel root, JPanel form, JScrollPane scroll, JTextField search, JButton btnSearch, JButton btnAdd, JButton btnUpdate, JButton btnDelete, JButton btnClear) {
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setPreferredSize(new Dimension(380, 0));
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBackground(SECONDARY);
        JPanel searchBar = new JPanel(new FlowLayout(0, 6, 4));
        searchBar.setBackground(SECONDARY);
        searchBar.add(new JLabel("Search:"));
        search.setFont(new Font("Segoe UI", 0, 12));
        searchBar.add(search);
        searchBar.add(btnSearch);
        right.add(searchBar, "North");
        right.add(scroll, "Center");
        JPanel btnBar = new JPanel(new FlowLayout(0, 6, 4));
        btnBar.setBackground(SECONDARY);
        btnBar.add(btnAdd);
        btnBar.add(btnUpdate);
        btnBar.add(btnDelete);
        btnBar.add(btnClear);
        right.add(btnBar, "South");
        JSplitPane split = new JSplitPane(1, formScroll, right);
        split.setDividerLocation(390);
        root.add(split, "Center");
        return root;
    }

    private void clearFields(JTextField... fields) {
        JTextField[] var2 = fields;
        int var3 = fields.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            JTextField f = var2[var4];
            f.setText("");
        }

    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    static {
        WHITE = Color.WHITE;
        GOLD = new Color(255, 193, 7);
        currentUserID = -1;
        currentUsername = "";
        currentRole = null;
    }

    public static enum Role {
        ADMIN,
        TEACHER,
        STUDENT;

        private Role() {
        }
    }
}
