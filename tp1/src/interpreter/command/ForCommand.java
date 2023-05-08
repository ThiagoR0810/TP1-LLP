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
        Value<?> e = expr.expr();
        boolean b = BoolValue.convert(e);
       for (var.initialize(e); b; var.expr()) {
            cmds.execute();
        }
    }
}
