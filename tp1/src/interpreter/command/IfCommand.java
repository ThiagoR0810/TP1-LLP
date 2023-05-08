package interpreter.command;

import interpreter.expr.Expr;

public class IfCommand extends Command {
    
    private Expr expr;
    private Command thenCmds;
    private Command elseCmds;

    public IfCommand (int line, Expr expr, Command thenCmds, Command elseCmds) {
        super(line);
        this.expr = expr;
        this.thenCmds = thenCmds;
        this.elseCmds = elseCmds;
    }

    @Override
    public void execute () {
        //Falta fazer
    }
}
