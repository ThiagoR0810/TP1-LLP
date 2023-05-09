package interpreter.expr;

import interpreter.value.Value;

public class CondicionalExpr extends Expr {
    
    private Expr cond;
    private Expr trueExpr;
    private Expr falseExpr;

    public CondicionalExpr (int line, Expr cond, Expr trueExpr, Expr falseExpr) {
        super(line);
        this.cond = cond;
        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;   
    }

    public Value<?> expr() {
        Value<?> condValue = cond.expr();
        if(condValue.value() instanceof Boolean){
            if((Boolean) condValue.value()){
                return trueExpr.expr();
            }else{
                return falseExpr.expr();
            }
        }else{
            throw new RuntimeException("Cond is not a boolean");
        }
    }
}
