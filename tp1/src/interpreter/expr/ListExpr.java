package interpreter.expr;

import java.util.List;

import interpreter.value.Value;

public class ListExpr extends Expr {
    
    private List<Expr> itens;

    public ListExpr (int line, List<Expr> itens) {
        super(line);
        this.itens = itens;
    }

    @Override
    public Value<?> expr(){
        return itens.get(getLine()).expr();
    }
}
