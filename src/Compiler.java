
import frontEnd.Lexer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) {
        try {
            String content = new String(Files.readAllBytes(Paths.get("testfile.txt")));
            Lexer lexer = Lexer.getInstance();
            lexer.analyze(content);
        } catch (Exception e) {
            System.err.println("Error reading source file: " + e.getMessage());
        }
    }
}
