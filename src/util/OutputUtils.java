package util;

import symbol.Symbol;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class OutputUtils {
    public static void outputSymbols(List<Symbol> symbols) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("symbol.txt"))) {
            for (Symbol symbol : symbols) {
                writer.write(symbol.getScopeLevel() + " " + symbol.getName() + " " + symbol.getTypeName());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
