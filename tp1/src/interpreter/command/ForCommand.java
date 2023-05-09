package interpreter.command;

import interpreter.expr.Expr;
import interpreter.expr.Variable;
import interpreter.value.BoolValue;
import interpreter.value.Value;

public class ForCommand extends Command {
    
    private Variable var;
    private Expr expr;
    private Command cmds;

    public ForCommand (int line, Variable var, Expr expr, Command cmds) {
        super(line);
        this.var = var;
        this.expr = expr;
        this.cmds = cmds;
    }

    @Override
    public void execute () {
        Value<?> value = expr.expr();
        if(value.getClass().isArray()){
            Value<?>[] array = (Value<?>[]) value.value();
            for (var.var v : array) {
                var.setValue(v);
                cmds.execute();
            }
        }else{
            throw new RuntimeException("Expr is not an iterable");
        }
    }
}
