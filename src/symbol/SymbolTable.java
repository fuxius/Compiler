package symbol;

import java.util.*;

public class SymbolTable {
    private static final SymbolTable instance = new SymbolTable();

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
    private Scope currentScope;
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
        scopeCounterForLLVM ++;
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
     */
    public int getCurrentScopeLevel() {
        return currentScope.getScopeLevel();
    }

    /**
     * 在当前作用域添加符号
     */
    public void addSymbol(Symbol symbol) {
        currentScope.getSymbols().put(symbol.getName(), symbol);
        symbolsInScope.get(currentScope.getScopeLevel()).add(symbol);
    }

    /**
     * 在当前作用域查找符号（用于检测重定义）
     */
    public Symbol lookupInCurrentScope(String name) {
        return currentScope.getSymbols().get(name);
    }

    /**
     * 全局查找符号（用于变量和函数的使用）
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
        private Map<String, Symbol> symbols = new LinkedHashMap<>();
        private Scope parentScope;
        private int scopeLevel;
        private List<Scope> childScopes = new ArrayList<>(); // 新增子作用域列表

        public Scope(Scope parentScope, int scopeLevel) {
            this.parentScope = parentScope;
            this.scopeLevel = scopeLevel;
            if (parentScope != null) {
                parentScope.addChildScope(this); // 将当前作用域添加到父作用域的子列表
            }
        }

        public Map<String, Symbol> getSymbols() {
            return symbols;
        }

        public Scope getParentScope() {
            return parentScope;
        }

        public int getScopeLevel() {
            return scopeLevel;
        }

        public List<Scope> getChildScopes() {
            return childScopes;
        }

        public void addChildScope(Scope childScope) {
            this.childScopes.add(childScope);
        }

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
