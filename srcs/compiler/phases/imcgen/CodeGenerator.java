/**
 * @author sliva
 */
package compiler.phases.imcgen;

import java.util.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.imcode.ImcBINOP;
import compiler.data.imcode.ImcCONST;
import compiler.data.imcode.ImcExpr;
import compiler.data.imcode.ImcUNOP;
import compiler.data.layout.*;
import compiler.phases.frames.*;

/**
 * Intermediate code generator.
 * 
 * This is a plain full visitor
 * 
 * @author sliva
 */
public class CodeGenerator extends AbsFullVisitor<Object, Stack<Frame>> {

    @Override
    public Object visit(AbsSource source, Stack<Frame> visArg){
        Stack<Frame> stack = new Stack<Frame>();
        return super.visit(source, stack);
    }

    @Override
    public Object visit(AbsFunDef fun, Stack<Frame> visArg){
        Frame frame = Frames.frames.get(fun);
        visArg.push(frame);
        super.visit(fun, visArg);
        visArg.pop();
        return null;
    }

    @Override
    public Object visit(AbsBinExpr expr, Stack<Frame> visArg){
        expr.fstExpr.accept(this, visArg);
        expr.sndExpr.accept(this, visArg);
        ImcExpr expr1 = ImcGen.exprImCode.get(expr.fstExpr);
        ImcExpr expr2 = ImcGen.exprImCode.get(expr.sndExpr);
        ImcBINOP.Oper oper = null;
        switch (expr.oper){
            case ADD: oper = ImcBINOP.Oper.ADD; break;
            case SUB: oper = ImcBINOP.Oper.SUB; break;
            case MUL: oper = ImcBINOP.Oper.MUL; break;
            case MOD: oper = ImcBINOP.Oper.MOD; break;
            case EQU: oper = ImcBINOP.Oper.EQU; break;
            case NEQ: oper = ImcBINOP.Oper.NEQ; break;
            case LEQ: oper = ImcBINOP.Oper.LEQ; break;
            case GEQ: oper = ImcBINOP.Oper.GEQ; break;
            case GTH: oper = ImcBINOP.Oper.GTH; break;
            case LTH: oper = ImcBINOP.Oper.LTH; break;
            case IOR: oper = ImcBINOP.Oper.IOR; break;
            case XOR: oper = ImcBINOP.Oper.XOR; break;
            case AND: oper = ImcBINOP.Oper.AND; break;
            case DIV: oper = ImcBINOP.Oper.DIV; break;
        }
        ImcBINOP binop = new ImcBINOP(oper, expr1, expr2);
        ImcGen.exprImCode.put(expr, binop);
        return binop;
    }

    @Override
    public Object visit(AbsUnExpr expr, Stack<Frame> visArg){
        switch (expr.oper){
            case ADD: {
                return expr.subExpr.accept(this, visArg);
            }
            case SUB: {
                expr.subExpr.accept(this, visArg);
                ImcExpr expr1 = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, new ImcUNOP(ImcUNOP.Oper.NEG, expr1));
                return null;
            }
            case NOT: {
                expr.subExpr.accept(this, visArg);
                ImcExpr expr1 = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, new ImcUNOP(ImcUNOP.Oper.NOT, expr1));
                return null;
            }
            // TODO: case data and address
        }
        return null;
    }

    @Override
    public Object visit(AbsAtomExpr atomExpr, Stack<Frame> visArg){
        switch (atomExpr.type){
            case PTR:
            case VOID: {
                ImcExpr atom = new ImcCONST(0);
                ImcGen.exprImCode.put(atomExpr, atom);
                return null;
            }
            case BOOL: {
                ImcExpr atom;
                if (atomExpr.expr.equals("false")){
                    atom = new ImcCONST(0);
                } else {
                    atom = new ImcCONST(1);
                }
                ImcGen.exprImCode.put(atomExpr, atom);
                return null;
            }
            case STR: {
                // TODO: fix
                return Frames.strings.get(atomExpr);
            }
        }
        return null;
    }

}
