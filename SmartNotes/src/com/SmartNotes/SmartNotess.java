package com.SmartNotes;

import java.io.*;
import java.sql.*;
import java.util.*;

public class SmartNotess {

    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.dir") + "/SmartNotes.db";

    public static void main(String[] args) {
        try {
            createDatabase();
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\n=== Flash card generator");
                System.out.println("1️⃣ Summarize new notes (Summary + Flashcards)");
                System.out.println("2️⃣ View previous summaries and flashcards");
                System.out.println("3️⃣ Exit");
                System.out.print("Choose an option: ");
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1:
                        summarizeNewNotes(sc);
                        break;
                    case 2:
                        viewPreviousSummaries(sc);
                        break;
                    case 3:
                        System.out.println("\n👋 Thank you for using Flashcard Generator. Goodbye!");
                        sc.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("❌ Invalid choice! Please try again.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //   Summarize new notes 
    public static void summarizeNewNotes(Scanner sc) {
        try {
            System.out.print("\nEnter full path of your .txt notes file: ");
            String path = sc.nextLine();

            String text = readTextFromFile(path);
            if (text.isEmpty()) {
                System.out.println("❌ File is empty or not found!");
                return;
            }

            System.out.println("\nGenerating summary...");
            String summary = summarize(text);
            System.out.println("\n📘 Summary:\n" + summary);

            System.out.println("\nGenerating flashcards...");
            List<String[]> flashcards = generateFlashcards(text);

            saveSummary(summary);
            saveSummaryToDatabase(summary);
            saveToDatabase(flashcards);
            exportToHTML(flashcards);

            System.out.println("\n✅ Summary and flashcards saved successfully!");
            try {
                java.awt.Desktop.getDesktop().browse(new java.io.File("flashcards.html").toURI());
            } catch (Exception e) {
                System.out.println("⚠ Could not open HTML automatically. Open manually from workspace folder.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  View previous summaries
    public static void viewPreviousSummaries(Scanner sc) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM summaries");

            System.out.println("\n📜 Previous Summaries:");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("\nSummary #" + rs.getInt("id") + ":\n" + rs.getString("summary_text"));
            }

            if (count == 0) {
                System.out.println("⚠ No old summaries found.");
            } else {
                System.out.print("\nWould you like to open the latest flashcards? (y/n): ");
                String ans = sc.nextLine().trim().toLowerCase();
                if (ans.equals("y")) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.io.File("flashcards.html").toURI());
                    } catch (Exception e) {
                        System.out.println("⚠ Could not open HTML automatically.");
                    }
                }
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read Text File
    public static String readTextFromFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append(" ");
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return sb.toString();
    }

    //  Summarize Content 
    public static String summarize(String text) {
        String[] sentences = text.split("(?<=[.!?]) ");
        int limit = Math.min(5, sentences.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++)
            sb.append(sentences[i]).append(" ");
        return sb.toString().trim();
    }

    // Generate Better Flashcards
    public static List<String[]> generateFlashcards(String text) {
        String[] sentences = text.split("(?<=[.!?]) ");
        List<String[]> flashcards = new ArrayList<>();

        for (int i = 0; i < Math.min(10, sentences.length); i++) {
            String sentence = sentences[i].trim();

            String[] words = sentence.split(" ");
            StringBuilder keyword = new StringBuilder();
            for (int j = 0; j < Math.min(4, words.length); j++) {
                keyword.append(words[j]).append(" ");
            }

            String question = "Q" + (i + 1) + ": What does this mean - \"" + keyword.toString().trim() + "\"?";
            String answer = sentence;
            flashcards.add(new String[]{question, answer});
        }
        return flashcards;
    }

    // Create Database 
    public static void createDatabase() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS flashcards (id INTEGER PRIMARY KEY AUTOINCREMENT, question TEXT, answer TEXT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS summaries (id INTEGER PRIMARY KEY AUTOINCREMENT, summary_text TEXT)");
        stmt.close();
        conn.close();
    }

    // Save Summary to Database 
    public static void saveSummaryToDatabase(String summary) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        String sql = "INSERT INTO summaries(summary_text) VALUES(?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, summary);
        pstmt.executeUpdate();
        pstmt.close();
        conn.close();
    }

    // Save Flashcards to Database 
    public static void saveToDatabase(List<String[]> flashcards) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        String sql = "INSERT INTO flashcards(question, answer) VALUES(?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        for (String[] card : flashcards) {
            pstmt.setString(1, card[0]);
            pstmt.setString(2, card[1]);
            pstmt.executeUpdate();
        }
        pstmt.close();
        conn.close();
    }

    // ---------- Save Formatted Summary to File ----------
    public static void saveSummary(String summary) throws IOException {
        String filePath = System.getProperty("user.dir") + "/summary.txt";
        summary = summary.replaceAll("(?<=[.!?]) ", "\n\n");
        FileWriter fw = new FileWriter(filePath);
        fw.write(summary);
        fw.close();
        System.out.println("✅ Summary written to: " + filePath);
    }

    // ---------- Export Flashcards to HTML ----------
    public static void exportToHTML(List<String[]> flashcards) throws IOException {
        String htmlPath = System.getProperty("user.dir") + "/flashcards.html";
        FileWriter fw = new FileWriter(htmlPath);
        fw.write("""
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset='UTF-8'>
                <style>
                body {font-family: Arial; text-align: center; background: #f5f5f5;}
                .card {width:300px; height:200px; margin:20px auto; perspective:1000px;}
                .inner {position:relative; width:100%; height:100%; transition:transform 0.6s; transform-style:preserve-3d;}
                .card:hover .inner {transform:rotateY(180deg);}
                .front, .back {position:absolute; width:100%; height:100%; backface-visibility:hidden;
                               display:flex; align-items:center; justify-content:center; border-radius:12px; font-size:16px;}
                .front {background:#4CAF50; color:white;}
                .back {background:white; color:black; transform:rotateY(180deg); border:1px solid #ccc;}
                </style>
                </head><body><h2>Flashcards</h2>
                """);
        for (String[] card : flashcards) {
            fw.write("<div class='card'><div class='inner'><div class='front'>" + card[0] +
                    "</div><div class='back'>" + card[1] + "</div></div></div>");
        }
        fw.write("</body></html>");
        fw.close();
        System.out.println("✅ Flashcards HTML created at: " + htmlPath);
    }
}