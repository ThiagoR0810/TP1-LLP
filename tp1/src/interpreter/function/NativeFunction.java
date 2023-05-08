package interpreter.function;

import interpreter.expr.Variable;
import interpreter.value.Value;

public class NativeFunction extends Function{
    
    private NativeOp op;

    public NativeFunction (Variable params, NativeOp op) {
        super(params);
        this.op = op;
    }

    public Value<?> call () {
        throw new RuntimeException("Me implemente!");
    } 
}
