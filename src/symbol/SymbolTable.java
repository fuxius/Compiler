package symbol;

import java.util.*;

/**
 * 符号表管理类，负责管理符号表栈和作用域信息。
 */
public class SymbolTable {
    // 符号表栈，每个作用域对应一个符号表（Map），键为符号名，值为符号对象
    private List<Map<String, Symbol>> symbolTableStack = new ArrayList<>();

    // 作用域序号计数器，初始为1（全局作用域）
    private int scopeCounter = 1;

    // 记录每个作用域的序号，用于输出时的排序
    private List<Integer> scopeLevels = new ArrayList<>();

    // 符号声明顺序记录，作用域序号 -> 符号列表（按声明顺序）
    private Map<Integer, List<Symbol>> symbolsInScope = new HashMap<>();

    public SymbolTable() {
        // 初始化全局作用域
        symbolTableStack.add(new LinkedHashMap<>());
        scopeLevels.add(scopeCounter);
        symbolsInScope.put(scopeCounter, new ArrayList<>());
    }

    /**
     * 进入新作用域，作用域序号加1
     */
    public void enterScope() {
        scopeCounter++;
        symbolTableStack.add(new LinkedHashMap<>());
        scopeLevels.add(scopeCounter);
        symbolsInScope.put(scopeCounter, new ArrayList<>());
    }

    /**
     * 退出当前作用域
     */
    public void exitScope() {
        if (symbolTableStack.size() > 0) {
            symbolTableStack.remove(symbolTableStack.size() - 1);
            scopeLevels.remove(scopeLevels.size() - 1);
        }
    }

    /**
     * 获取当前作用域的序号
     */
    public int getCurrentScopeLevel() {
        if (scopeLevels.size() > 0) {
            return scopeLevels.get(scopeLevels.size() - 1);
        }
        return 1; // 默认返回全局作用域
    }

    /**
     * 在当前作用域添加符号
     */
    public void addSymbol(Symbol symbol) {
        Map<String, Symbol> currentScope = symbolTableStack.get(symbolTableStack.size() - 1);
        currentScope.put(symbol.getName(), symbol);

        // 记录符号的声明顺序
        int currentScopeLevel = getCurrentScopeLevel();
        symbolsInScope.get(currentScopeLevel).add(symbol);
    }

    /**
     * 在当前作用域查找符号（用于检测重定义）
     */
    public Symbol lookupInCurrentScope(String name) {
        Map<String, Symbol> currentScope = symbolTableStack.get(symbolTableStack.size() - 1);
        return currentScope.get(name);
    }

    /**
     * 全局查找符号（用于变量和函数的使用）
     */
    public Symbol lookup(String name) {
        for (int i = symbolTableStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = symbolTableStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    /**
     * 获取所有符号，按作用域序号从小到大排序
     */
    public List<Symbol> getAllSymbols() {
        List<Symbol> allSymbols = new ArrayList<>();
        List<Integer> sortedScopeLevels = new ArrayList<>(symbolsInScope.keySet());
        Collections.sort(sortedScopeLevels);

        for (Integer scopeLevel : sortedScopeLevels) {
            List<Symbol> symbols = symbolsInScope.get(scopeLevel);
            allSymbols.addAll(symbols);
        }
        return allSymbols;
    }
}
