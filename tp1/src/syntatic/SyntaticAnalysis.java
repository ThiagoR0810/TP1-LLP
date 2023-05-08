
package syntatic;

import static lexical.Token.Type.ADD;
import static lexical.Token.Type.AND;
import static lexical.Token.Type.ASSIGN;
import static lexical.Token.Type.CLOSE_BRA;
import static lexical.Token.Type.CLOSE_CUR;
import static lexical.Token.Type.CLOSE_PAR;
import static lexical.Token.Type.COLON;
import static lexical.Token.Type.COMMA;
import static lexical.Token.Type.CONST;
import static lexical.Token.Type.DEBUG;
import static lexical.Token.Type.DEC;
import static lexical.Token.Type.DIV;
import static lexical.Token.Type.ELSE;
import static lexical.Token.Type.END_OF_FILE;
import static lexical.Token.Type.FALSE;
import static lexical.Token.Type.FOR;
import static lexical.Token.Type.FUNCTION;
import static lexical.Token.Type.IF;
import static lexical.Token.Type.IN;
import static lexical.Token.Type.INC;
import static lexical.Token.Type.LET;
import static lexical.Token.Type.MUL;
import static lexical.Token.Type.NAME;
import static lexical.Token.Type.NOT;
import static lexical.Token.Type.NUMBER;
import static lexical.Token.Type.OPEN_BRA;
import static lexical.Token.Type.OPEN_CUR;
import static lexical.Token.Type.OPEN_PAR;
import static lexical.Token.Type.OR;
import static lexical.Token.Type.RETURN;
import static lexical.Token.Type.SEMICOLON;
import static lexical.Token.Type.SUB;
import static lexical.Token.Type.TEXT;
import static lexical.Token.Type.TRUE;
import static lexical.Token.Type.UNDEFINED;
import static lexical.Token.Type.WHILE;

import java.util.ArrayList;
import java.util.List;

import interpreter.Environment;
import interpreter.Interpreter;
import interpreter.InterpreterException;
import interpreter.command.AssignCommand;
import interpreter.command.BlocksCommand;
import interpreter.command.Command;
import interpreter.command.DebugCommand;
import interpreter.command.InitializeCommand;
import interpreter.command.WhileCommand;
import interpreter.expr.BinaryExpr;
import interpreter.expr.ConstExpr;
import interpreter.expr.Expr;
import interpreter.expr.FunctionCallExpr;
import interpreter.expr.SetExpr;
import interpreter.expr.UnaryExpr;
import interpreter.expr.Variable;
import interpreter.function.StandardFunction;
import interpreter.value.BoolValue;
import interpreter.value.FunctionValue;
import interpreter.value.Value;
import lexical.LexicalAnalysis;
import lexical.Token;

public class SyntaticAnalysis {

    private LexicalAnalysis lex;
    private Token current;
    private Token previous;
    private Environment environment;

    public SyntaticAnalysis(LexicalAnalysis lex) {
        this.lex = lex;
        this.current = lex.nextToken();
        this.previous = null;
        this.environment = Interpreter.globals;
    }

    public Command process() {
        Command cmd = procCode();
        eat(END_OF_FILE);

        return cmd;
    }

    private void advance() {
        //System.out.println("Found " + current);
        previous = current;
        current = lex.nextToken();
    }

    private void eat(Token.Type type) {
        if (type == current.type) {
            advance();
        } else {
            // System.out.println("Expected (..., " + type + ", ..., ...), found " + current);
            reportError();
      }
    }

    private boolean check(Token.Type ...types) {
        for (Token.Type type : types) {
            if (current.type == type)
                return true;
        }

        return false;
    }

    private boolean match(Token.Type ...types) {
        if (check(types)) {
            advance();
            return true;
        } else {
            return false;
        }
    }

     private void reportError() {
        String reason;
        switch (current.type) {
            case INVALID_TOKEN:
                reason = String.format("Lexema inválido [%s]", current.lexeme);
                break;
            case UNEXPECTED_EOF:
            case END_OF_FILE:
                reason = "Fim de arquivo inesperado";
                break;
            default:
                reason = String.format("Lexema não esperado [%s]", current.lexeme);
                break;
        }

        throw new SyntaticException(current.line, reason);
    }

    // <code> ::= { <cmd> }
    private BlocksCommand procCode() {
        List<Command> cmds = new ArrayList<Command>();
        int line = current.line;
        while (check(OPEN_CUR, CONST, LET, DEBUG,
                IF, WHILE, FOR, NOT, ADD, SUB,
                INC, DEC, OPEN_PAR, UNDEFINED,
                FALSE, TRUE, NUMBER, TEXT,
                OPEN_BRA, OPEN_CUR, FUNCTION, NAME)) {
            Command cmd = procCmd();
            cmds.add(cmd);
        }

        return new BlocksCommand(line, cmds);
    }

    // <cmd> ::= <block> | <decl> | <debug> | <if> | <while> | <for> | <assign>
    private Command procCmd() {
        Command cmd = null;
        if (check(OPEN_CUR)) {
            cmd = procBlock();
        } else if (check(CONST, LET)) {
            cmd = procDecl();
        } else if (check(DEBUG)) {
            cmd = procDebug();
        } else if (check(IF)) {
            procIf();
        } else if (check(WHILE)) {
            cmd = procWhile();
        } else if (check(FOR)) {
            procFor();
        } else {
            cmd = procAssign();
        }

        return cmd;
    }

    // <block> ::= '{' <code> '}'
    private BlocksCommand procBlock() {
        eat(OPEN_CUR);

        Environment old = this.environment;
        this.environment = new Environment(old);

        BlocksCommand bcmds = null;
        try {
            bcmds = procCode();
        } finally {
            this.environment = old;
        }

        eat(CLOSE_CUR);

        return bcmds;
    }

    // <decl> ::= ( const | let ) <name> [ '=' <expr> ] { ',' <name> [ '=' <expr> ] } ';'
    private BlocksCommand procDecl() {
        boolean constant = false;
        if (match(CONST, LET)) {
            constant = (previous.type == CONST);
        } else {
            reportError();
        }
        int line = previous.line;
        
        Token name = procName();
        Variable var = this.environment.declare(name, constant);

        Expr expr = match(ASSIGN) ?
            procExpr() : new ConstExpr(name.line, null);

        InitializeCommand icmd = new InitializeCommand(name.line, var, expr);

        List<Command> cmds = new ArrayList<Command>();
        cmds.add(icmd);

        while (match(COMMA)) {
            name = procName();
            var = this.environment.declare(name, constant);

            expr = match(ASSIGN) ?
                procExpr() : new ConstExpr(name.line, null);

            icmd = new InitializeCommand(name.line, var, expr);
            cmds.add(icmd);
        }

        eat(SEMICOLON);

        BlocksCommand bcmds = new BlocksCommand(line, cmds);
        return bcmds;
    }

    // <debug> ::= debug <expr> ';'
    private DebugCommand procDebug() {
        eat(DEBUG);
        int line = previous.line;

        Expr expr = procExpr();
        eat(SEMICOLON);

        return new DebugCommand(line, expr);
    }

    // <if> ::= if '(' <expr> ')' <cmd> [ else <cmd> ]
    private void procIf() {
        eat(IF);
        eat(OPEN_PAR);
        procExpr();
        eat(CLOSE_PAR);
        procCmd();
        if (match(ELSE)) {
            procCmd();
        }
    }

    // <while> ::= while '(' <expr> ')' <cmd>
    private WhileCommand procWhile() {
        eat(WHILE);
        int line = previous.line;

        eat(OPEN_PAR);
        Expr expr = procExpr();
        eat(CLOSE_PAR);
        Command cmds = procCmd();

        WhileCommand wcmd = new WhileCommand(line, expr, cmds);
        return wcmd;
    }

  // <for> ::= for '(' [ let ] <name> in <expr> ')' <cmd>
  private void procFor() {
      eat(FOR);
      eat(OPEN_PAR);
      if (match(LET)) {
          // fazer nada
      }
      procName();
      eat(IN);
      procExpr();
      eat(CLOSE_PAR);
      procCmd();
  }

  // <assign> ::= [ <expr> '=' ] <expr> ';'
  private AssignCommand procAssign() {
      int line = current.line;
      Expr rhs = procExpr();

      SetExpr lhs = null;
      if (match(ASSIGN)) {
          if (!(rhs instanceof SetExpr))
              throw new InterpreterException(line);

          lhs = (SetExpr) rhs;
          rhs = procExpr();
      }

      eat(SEMICOLON);

      AssignCommand acmd = new AssignCommand(line, rhs, lhs);
      return acmd;
  }

    // <expr> ::= <cond> [ '?' <expr> ':' <expr> ]
    private Expr procExpr() {
        Expr expr = procCond();
        
        if (match(Token.Type.TERNARY)) {
            expr = procExpr();
        } else if (match(Token.Type.COLON)) {
            expr = procExpr();
        }

        return expr;
    }

    // <cond> ::= <rel> { ( '&&' | '||' ) <rel> }
    private Expr procCond() {
        Expr expr = procRel();
        while (match(AND, OR)) {
            // TODO: Me implementar.
            BinaryExpr.Op op;
            switch (previous.type) {
                case AND:
                    op = BinaryExpr.Op.And;
                    break;
                case OR:
                default:
                    op = BinaryExpr.Op.Or;
                    break;
            }
            procRel();
        }

        return expr;
    }

    // <rel> ::= <arith> [ ( '<' | '>' | '<=' | '>=' | '==' | '!=' ) <arith> ]
    private Expr procRel() {
        Expr expr = procArith();

        if (match(Token.Type.GREATER_THAN, Token.Type.LOWER_THAN,
                    Token.Type.GREATER_EQUAL, Token.Type.LOWER_EQUAL, 
                    Token.Type.EQUALS, Token.Type.NOT_EQUALS)) {
                expr = procArith();
        }

        return expr;
    }

    // <arith> ::= <term> { ( '+' | '-' ) <term> }
    private Expr procArith() {
        Expr left = procTerm();
        while (match(ADD, SUB)) {
            BinaryExpr.Op op;
            switch (previous.type) {
                case ADD:
                    op = BinaryExpr.Op.Add;
                    break;
                case SUB:
                default:
                    op = BinaryExpr.Op.Sub;
                    break;
            }

            int line = previous.line;

            Expr right = procTerm();

            left = new BinaryExpr(line, left, op, right);
        }

        return left;
    }

    // <term> ::= <prefix> { ( '*' | '/' ) <prefix> }
    private Expr procTerm() {
        Expr left = procPrefix();

        while (match(MUL, DIV)) {
            BinaryExpr.Op op;
            switch (previous.type) {
                case MUL:
                    op = BinaryExpr.Op.Mul;
                    break;
                case DIV:
                default:
                    op = BinaryExpr.Op.Div;
                    break;
            }

            int line = previous.line;

            Expr right = procPrefix();

            left = new BinaryExpr(line, left, op, right);
        }

        return left;
    }

    // <prefix> ::= [ '!' | '+' | '-' | '++' | '--' ] <factor>
    private Expr procPrefix() {
        Token token = null;
        if (match(NOT, ADD, SUB, INC, DEC)) {
            token = previous;
        }

        Expr expr = procFactor();

        if (token != null) {
            UnaryExpr.Op op;
            switch (token.type) {
                case NOT:
                    op = UnaryExpr.Op.Not;
                    break;
                case ADD:
                    op = UnaryExpr.Op.Pos;
                    break;
                case SUB:
                    op = UnaryExpr.Op.Neg;
                    break;
                case INC:
                    op = UnaryExpr.Op.PreInc;
                    break;
                case DEC:
                default:
                    op = UnaryExpr.Op.PreDec;
                    break;
            }

            UnaryExpr uexpr = new UnaryExpr(previous.line,
                expr, op);
            expr = uexpr;
        }

        return expr;
    }

    // <factor> ::= ( '(' <expr> ')' | <rvalue> ) <calls>  [ '++' | '--' ]
    private Expr procFactor() {
        Expr expr = null;
        if (match(OPEN_PAR)) {
            expr = procExpr();
            eat(CLOSE_PAR);
        } else {
            expr = procRValue();
        }

        expr = procCalls(expr);

        if (match(INC, DEC)) {
            // fazer nada
        }

        return expr;
    }

    // <rvalue> ::= <const> | <list> | <object> | <function> | <lvalue>
    private Expr procRValue() {
        Expr expr = null;
        if (check(UNDEFINED, FALSE, TRUE, NUMBER, TEXT)) {
            Value<?> v = procConst();
            expr = new ConstExpr(previous.line, v);
        } else if (check(OPEN_BRA)) {
            procList();
        } else if (check(OPEN_CUR)) {
            procObject();
        } else if (check(FUNCTION)) {
            int line = current.line;
            StandardFunction sf = procFunction();
            FunctionValue fv = new FunctionValue(sf);
            expr = new ConstExpr(line, fv);
        } else {
            expr = procLValue();
        }

        return expr;
    }

    // <const> ::= undefined | false | true | <number> | <text>
    private Value<?> procConst() {
        Value<?> v = null;
        if (match(UNDEFINED, FALSE, TRUE)) {
            switch (previous.type) {
                case UNDEFINED:
                    v = null;
                    break;
                case FALSE:
                    v = new BoolValue(false);
                    break;
                case TRUE:
                default:
                    v = new BoolValue(true);
                    break;
            }
            // fazer nada
        } else if (check(NUMBER)) {
            v = procNumber();
        } else if (check(TEXT)) {
            v = procText();
        } else {
            reportError();
        }

        return v;
    }

    // <list> ::= '[' [ <expr> { ',' <expr> } ] ']'
    private void procList() {
        eat(OPEN_BRA);

        if (check(NOT, ADD, SUB, INC, DEC, OPEN_PAR,
                UNDEFINED, FALSE, TRUE, NUMBER, TEXT, OPEN_BRA,
                OPEN_CUR, FUNCTION, NAME)) {
            procExpr();

            while (match(COMMA)) {
                procExpr();
            }
        }

        eat(CLOSE_BRA);
    }

    // <object> ::= '{' [ <name> ':' <expr> { ',' <name> ':' <expr> } ] '}'
    private void procObject() {
        eat(OPEN_CUR);

        if (check(NAME)) {
            procName();
            eat(COLON);
            procExpr();

            while (match(COMMA)) {
                procName();
                eat(COLON);
                procExpr();
            }
        }

        eat(CLOSE_CUR);
    }

    // <function> ::= function '(' ')' '{' <code> [ return <expr> ';' ] '}'
    private StandardFunction procFunction() {
        eat(FUNCTION);
        eat(OPEN_PAR);
        eat(CLOSE_PAR);
        eat(OPEN_CUR);

        Environment old = this.environment;
        this.environment = new Environment(old);

        StandardFunction sf = null;
        try {
            Variable params = this.environment.declare(
                new Token("params", Token.Type.NAME, null),
                false);
        
            Command cmds = procCode();
            Expr ret = null;
            if (match(RETURN)) {
                ret = procExpr();
                eat(SEMICOLON);
            }

            sf = new StandardFunction(params, cmds, ret);
        } finally {
            this.environment = old;
        }
        eat(CLOSE_CUR);

        return sf;
    }

    // <lvalue> ::= <name> { '.' <name> | '[' <expr> ']' }
    private SetExpr procLValue() {
        Token name = procName();
        Variable var = this.environment.get(name);

        while (check(Token.Type.DOT, OPEN_BRA)) {
            if (match(Token.Type.DOT)) {
                procName();
            } else {
                eat(OPEN_BRA);
                procExpr();
                eat(CLOSE_BRA);
            }
        }

        return var;
    }

    // <calls> ::= { '(' [ <expr> { ',' <expr> } ] ')' }
    private Expr procCalls(Expr expr) {
        while (match(OPEN_PAR)) {
            int line = previous.line;

            List<Expr> args = new ArrayList<Expr>();
            if (check(NOT, ADD, SUB, INC, DEC, OPEN_PAR,
                    UNDEFINED, FALSE, TRUE, NUMBER, TEXT, OPEN_BRA,
                    OPEN_CUR, FUNCTION, NAME)) {
                Expr a = procExpr();
                args.add(a);
                while (match(COMMA)) {
                    a = procExpr();
                    args.add(a);
                }
            }

            eat(CLOSE_PAR);

            expr = new FunctionCallExpr(line, expr, args);
        }
        
        return expr;
    }

    private Value<?> procNumber() {
        eat(NUMBER);
        return previous.literal;
    }

    private Value<?> procText() {
        eat(TEXT);
        return previous.literal;
    }

    private Token procName() {
        eat(NAME);
        return previous;
    }

}
