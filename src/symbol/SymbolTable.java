package symbol;

import java.util.*;

/**
 * SymbolTable类用于管理符号表，支持作用域嵌套和符号查找。
 */
public class SymbolTable {
    private static final SymbolTable instance = new SymbolTable(); // SymbolTable单例对象

    // 私有化构造器，防止外部实例化
    private SymbolTable() {
        // 初始化全局作用域
        currentScope = new Scope(null, scopeCounter);
        symbolsInScope.put(scopeCounter, new ArrayList<>());
    }

    // 提供全局访问点
    public static SymbolTable getInstance() {
        return instance;
    }

    private Scope currentScope; // 当前作用域
    private int scopeCounter = 1; // 从1开始，表示全局作用域
    private int scopeCounterForLLVM = 1; // 从1开始，表示全局作用域

    // 记录每个作用域的符号列表，按作用域序号索引
    private Map<Integer, List<Symbol>> symbolsInScope = new HashMap<>();

    /**
     * 进入新作用域
     */
    public void enterScope() {
        scopeCounter++;
        currentScope = new Scope(currentScope, scopeCounter);
        symbolsInScope.put(scopeCounter, new ArrayList<>());
    }

    /**
     * 进入已建立的子作用域
     */
    public void enterScopeForLLVM() {
        scopeCounterForLLVM++;
        Scope targetScope = currentScope.getChildScopeByLevel(scopeCounterForLLVM);
        if (targetScope != null) {
            currentScope = targetScope;
        } else {
            throw new IllegalArgumentException("Target scope " + scopeCounterForLLVM + " does not exist or is not a child of the current scope.");
        }
    }

    /**
     * LLVM退出当前作用域
     */
    public void exitScopeForLLVM() {
        if (currentScope.getParentScope() != null) {
            currentScope = currentScope.getParentScope();
        } else {
            // 已经在全局作用域，无法再退出
            System.err.println("Cannot exit the global scope.");
        }
    }

    /**
     * 退出当前作用域
     */
    public void exitScope() {
        if (currentScope.getParentScope() != null) {
            currentScope = currentScope.getParentScope();
        } else {
            // 已经在全局作用域，无法再退出
            System.err.println("Cannot exit the global scope.");
        }
    }

    /**
     * 获取当前作用域的序号
     * @return 当前作用域的序号
     */
    public int getCurrentScopeLevel() {
        return currentScope.getScopeLevel();
    }

    /**
     * 在当前作用域添加符号
     * @param symbol 要添加的符号
     */
    public void addSymbol(Symbol symbol) {
        currentScope.getSymbols().put(symbol.getName(), symbol);
        symbolsInScope.get(currentScope.getScopeLevel()).add(symbol);
    }

    /**
     * 在当前作用域查找符号（用于检测重定义）
     * @param name 符号名称
     * @return 找到的符号，或null如果未找到
     */
    public Symbol lookupInCurrentScope(String name) {
        return currentScope.getSymbols().get(name);
    }

    /**
     * 全局查找符号（用于变量和函数的使用）
     * @param name 符号名称
     * @return 找到的符号，或null如果未找到
     */
    public Symbol lookup(String name) {
        Scope scope = currentScope;
        while (scope != null) {
            Symbol symbol = scope.getSymbols().get(name);
            if (symbol != null) {
                return symbol;
            }
            scope = scope.getParentScope();
        }
        return null;
    }

    /**
     * 获取所有符号，按作用域序号从小到大排序
     * @return 所有符号的列表
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

    /**
     * 判断当前作用域是否为函数作用域
     * @return 如果当前作用域为函数作用域则返回true，否则返回false
     */
    public boolean isCurrentScopeFunction() {
        for (Symbol symbol : currentScope.getSymbols().values()) {
            if (symbol.isFunction()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 内部类，表示一个作用域
     */
    private static class Scope {
        private Map<String, Symbol> symbols = new LinkedHashMap<>(); // 符号表
        private Scope parentScope; // 父作用域
        private int scopeLevel; // 作用域层级
        private List<Scope> childScopes = new ArrayList<>(); // 子作用域列表

        /**
         * 构造函数
         * @param parentScope 父作用域
         * @param scopeLevel 作用域层级
         */
        public Scope(Scope parentScope, int scopeLevel) {
            this.parentScope = parentScope;
            this.scopeLevel = scopeLevel;
            if (parentScope != null) {
                parentScope.addChildScope(this); // 将当前作用域添加到父作用域的子列表
            }
        }

        /**
         * 获取符号表
         * @return 符号表
         */
        public Map<String, Symbol> getSymbols() {
            return symbols;
        }

        /**
         * 获取父作用域
         * @return 父作用域
         */
        public Scope getParentScope() {
            return parentScope;
        }

        /**
         * 获取作用域层级
         * @return 作用域层级
         */
        public int getScopeLevel() {
            return scopeLevel;
        }

        /**
         * 获取子作用域列表
         * @return 子作用域列表
         */
        public List<Scope> getChildScopes() {
            return childScopes;
        }

        /**
         * 添加子作用域
         * @param childScope 子作用域
         */
        public void addChildScope(Scope childScope) {
            this.childScopes.add(childScope);
        }

        /**
         * 根据层级获取子作用域
         * @param scopeLevel 作用域层级
         * @return 子作用域，或null如果未找到
         */
        public Scope getChildScopeByLevel(int scopeLevel) {
            for (Scope child : childScopes) {
                if (child.getScopeLevel() == scopeLevel) {
                    return child;
                }
            }
            return null; // 如果未找到子作用域，返回 null
        }
    }
}