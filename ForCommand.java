package interpreter.command;

import interpreter.expr.Expr;
import interpreter.expr.Variable;

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
        // Falta fazer
    }
}
