package interpreter.expr;

import java.util.List;

import interpreter.value.Value;

public class ObjectExpr extends Expr{
    
    private List<ObjectItem> items;

    public ObjectExpr (int line, List<ObjectItem> items) {
        super(line);
        this.items = items;
    }

    public Value<?> expr () {
        throw new RuntimeException("Me implemente!");
    }
    
}
