package interpreter.expr;

import interpreter.value.Value;

public class AccessExpr extends SetExpr {
    
    private SetExpr base;
    private Expr index;

    public AccessExpr (int line, SetExpr base, Expr index) {
        super(line);
        this.base = base;
        this.index = index;
    }

    @Override
    public Value<?> expr(){
        return index.expr();
    }

    @Override
    public void setValue( Value<?> value) {
        base.setValue(value);
    }
}
