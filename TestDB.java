import java.sql.*;
public class TestDB {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing connection...");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://hopper.proxy.rlwy.net:19507/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=60000",
            "root",
            "oXgdZdeMhQaPQGiFmxSIHuiQCNeCamQr"
        );
        System.out.println("Connected successfully!");
        conn.close();
    }
}