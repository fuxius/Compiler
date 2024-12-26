package error;

import frontEnd.Parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all")
public class ErrorHandler {
    private static final ErrorHandler instance = new ErrorHandler();

    public static ErrorHandler getInstance() {
        return instance;
    }
    private List<String> errors = new ArrayList<>();

    public boolean errorOccured() {
        return !errors.isEmpty();
    }

    public void reportError(int line, ErrorType type) {
        errors.add(line + " " + type.getCode());
    }

    public List<String> getErrors() {
        return errors;
    }

    public void clear() {
        errors.clear();
    }

    // 将错误输出到 error.txt
    public void outputErrors()throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("error.txt"))) {
            // 自定义排序，按行号（数字）升序排序
            Collections.sort(errors, new Comparator<String>() {
                @Override
                public int compare(String error1, String error2) {
                    // 提取行号
                    int line1 = Integer.parseInt(error1.split(" ")[0]);
                    int line2 = Integer.parseInt(error2.split(" ")[0]);
                    return Integer.compare(line1, line2);
                }
            });

            // 输出排序后的错误
            for (String error : errors) {
                String type = error.split(" ")[1];
//                if(!type.equals("e")) {
//                    writer.write(error + "\n");
//                }
                writer.write(error + "\n");
            }
//            if(errors.size() > 3) {
//                throw new IOException();
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
