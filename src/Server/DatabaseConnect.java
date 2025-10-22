package Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseConnect {
    private static final String URL = "jdbc:mysql://localhost:3306/qlsv";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // nếu có mật khẩu thì điền vào

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối CSDL: " + e.getMessage());
            return null;
        }
    }

    public static void insertStudent(String id, String name, String major, double gpa) {
        String sql = "INSERT INTO sinhvien (id, name, major, gpa) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, major);
            stmt.setDouble(4, gpa);
            stmt.executeUpdate();

            System.out.println("✅ Đã thêm sinh viên vào CSDL: " + name);
        } catch (SQLException e) {
            System.err.println("❌ Lỗi thêm sinh viên: " + e.getMessage());
        }
    }

    public static List<Map<String, String>> getAllStudents() {
        List<Map<String, String>> students = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM SINHVIEN";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> student = new HashMap<>();
                student.put("id", rs.getString("id"));
                student.put("name", rs.getString("name"));
                student.put("major", rs.getString("major"));
                student.put("gpa", rs.getString("gpa"));
                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public static void deleteStudent(String id) {
        String sql = "DELETE FROM sinhvien WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0)
                System.out.println("🗑️ Đã xóa sinh viên ID: " + id);
            else
                System.out.println("⚠️ Không tìm thấy sinh viên ID: " + id);
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi xóa sinh viên: " + e.getMessage());
        }
    }


}
