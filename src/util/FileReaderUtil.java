package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileReaderUtil {

    /**
     * 读取源文件并将其转换为按行存储的列表
     * @param filePath 源文件路径
     * @return 按行存储的字符串列表
     * @throws IOException 文件读取异常
     */
    public static List<String> readLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();  // 存储每行内容

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);  // 将每一行添加到列表中
            }
        }

        return lines;  // 返回行列表
    }
}
