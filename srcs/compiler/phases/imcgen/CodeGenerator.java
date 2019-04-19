/**
 * @author sliva
 */
package compiler.phases.imcgen;

import java.util.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.imcode.*;
import compiler.data.layout.*;
import compiler.data.type.SemCharType;
import compiler.data.type.SemType;
import compiler.phases.frames.*;
import compiler.phases.seman.SemAn;

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
            case INT: {
                ImcExpr atom = new ImcCONST(Integer.parseInt(atomExpr.expr));
                ImcGen.exprImCode.put(atomExpr, atom);
                return null;
            }
            case CHAR: {
                char character = atomExpr.expr.charAt(1);
                ImcExpr atom = new ImcCONST((long)character);
                ImcGen.exprImCode.put(atomExpr, atom);
                return null;
            }
            case STR: {
                // TODO: fix
                AbsAccess access = Frames.strings.get(atomExpr);
                ImcExpr name = new ImcNAME(access.label);
                ImcGen.exprImCode.put(atomExpr, new ImcMEM(name));
                return null;
            }
        }
        return null;
    }

    @Override
    public Object visit(AbsVarName varName, Stack<Frame> visArg){
        Access varAcces = Frames.accesses.get((AbsVarDecl) SemAn.declaredAt.get(varName));
        if (varAcces instanceof AbsAccess){
            ImcExpr address = new ImcNAME(((AbsAccess) varAcces).label);
            ImcGen.exprImCode.put(varName, new ImcMEM(address));
            return null;
        } else {
            RelAccess access = (RelAccess) varAcces;
            ImcExpr addr = new ImcTEMP(visArg.peek().FP);
            for (int i=access.depth; i<visArg.peek().depth; i++){
                addr = new ImcMEM(addr);
            }
            addr = new ImcBINOP(ImcBINOP.Oper.ADD, addr, new ImcCONST(access.offset));
            ImcGen.exprImCode.put(varName, new ImcMEM(addr));
            return null;
        }
    }

    @Override
    public Object visit(AbsArrExpr arrExpr, Stack<Frame> visArg){
        // Maybe check if it is an address
        arrExpr.array.accept(this, visArg);
        ImcMEM exprLeft = (ImcMEM) ImcGen.exprImCode.get(arrExpr.array);
        ImcExpr addr1 = exprLeft.addr;
        arrExpr.array.accept(this, visArg);
        ImcCONST index = (ImcCONST) ImcGen.exprImCode.get(arrExpr.index);
        ImcCONST arrSize = new ImcCONST(SemAn.ofType.get(arrExpr).size());
        ImcBINOP indexVal = new ImcBINOP(ImcBINOP.Oper.MUL, index, arrSize);
        ImcBINOP arrAddr = new ImcBINOP(ImcBINOP.Oper.ADD, addr1, indexVal);
        ImcGen.exprImCode.put(arrExpr, new ImcMEM(arrAddr));
        return null;
    }

    @Override
    public Object visit(AbsRecExpr expr, Stack<Frame> visArg){
        expr.record.accept(this, visArg);
        ImcMEM rec = (ImcMEM) ImcGen.exprImCode.get(expr.record);
        ImcExpr recAddr = rec.addr;
        //RelAccess access = (RelAccess) Frames.accesses.get((AbsVarDecl) SemAn.declaredAt.get(expr.comp));
        // TODO: Fix
        RelAccess access = new RelAccess(8, 0, 0);
        ImcBINOP compAddr = new ImcBINOP(ImcBINOP.Oper.ADD, recAddr, new ImcCONST(access.offset));
        ImcGen.exprImCode.put(expr, new ImcMEM(compAddr));
        return compAddr;
    }

    @Override
    public Object visit(AbsUnExpr expr, Stack<Frame> visArg){
        switch (expr.oper){
            case ADD: {
                expr.subExpr.accept(this, visArg);
                ImcExpr subExpr = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, subExpr);
                return null;
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
            case DATA: {
                expr.subExpr.accept(this, visArg);
                ImcExpr subExpr = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, new ImcMEM(subExpr));
                return null;
            }
            case ADDR: {
                expr.subExpr.accept(this, visArg);
                ImcMEM subExpr = (ImcMEM) ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, subExpr.addr);
            }
        }
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
    public Object visit(AbsNewExpr newExpr, Stack<Frame> visArg){
        SemType type = SemAn.isType.get(newExpr.type);
        Vector<ImcExpr> args = new Vector<>();
        args.add(new ImcCONST(type.size()));
        ImcCALL call = new ImcCALL(new Label("new"), args);
        ImcGen.exprImCode.put(newExpr, call);
        return null;
    }

    @Override
    public Object visit(AbsDelExpr delExpr, Stack<Frame> visArg){
        delExpr.expr.accept(this, visArg);
        ImcExpr expr = ImcGen.exprImCode.get(delExpr.expr);
        Vector<ImcExpr> args = new Vector<>();
        args.add(expr);
        ImcCALL call = new ImcCALL(new Label("del"), args);
        ImcGen.exprImCode.put(delExpr, call);
        return null;
    }

    @Override
    public Object visit(AbsFunName funName, Stack<Frame> visArg){
        Vector<ImcExpr> args = new Vector<>();
        for (AbsExpr expr : funName.args.args()){
            expr.accept(this, visArg);
            args.add(ImcGen.exprImCode.get(expr));
        }
        Label label = Frames.frames.get((AbsFunDecl) SemAn.declaredAt.get(funName)).label;
        ImcGen.exprImCode.put(funName, new ImcCALL(label, args));
        return null;
    }

    @Override
    public Object visit(AbsBlockExpr blockExpr, Stack<Frame> visArg){
        blockExpr.decls.accept(this, visArg);
        ImcStmt stmts = (ImcStmt) blockExpr.stmts.accept(this, visArg);
        blockExpr.expr.accept(this, visArg);
        ImcExpr expr = ImcGen.exprImCode.get(blockExpr.expr);
        ImcGen.exprImCode.put(blockExpr, new ImcSEXPR(stmts, expr));
        return null;
    }

    @Override
    public Object visit(AbsStmts stmts, Stack<Frame> visArg){
        Vector<ImcStmt> imcStmts = new Vector<>();
        for (AbsStmt stmt : stmts.stmts()){
            stmt.accept(this, visArg);
            imcStmts.add(ImcGen.stmtImCode.get(stmt));
        }
        return imcStmts;
    }

    @Override
    public Object visit(AbsCastExpr castExpr, Stack<Frame> visArg){
        SemType type = SemAn.isType.get(castExpr.type);
        castExpr.expr.accept(this, visArg);
        ImcExpr expr = ImcGen.exprImCode.get(castExpr.expr);
        if (type instanceof SemCharType){
            ImcGen.exprImCode.put(castExpr, new ImcBINOP(ImcBINOP.Oper.MOD, expr, new ImcCONST(256)));
        } else {
            ImcGen.exprImCode.put(castExpr, expr);
        }
        return null;
    }

    @Override
    public Object visit(AbsExprStmt exprStmt, Stack<Frame> visArg){
        exprStmt.expr.accept(this, visArg);
        ImcGen.stmtImCode.put(exprStmt, new ImcESTMT(ImcGen.exprImCode.get(exprStmt.expr)));
        return null;
    }

    @Override
    public Object visit(AbsAssignStmt assignStmt, Stack<Frame> visArg){
        assignStmt.dst.accept(this, visArg);
        assignStmt.src.accept(this, visArg);
        ImcExpr dstExpr = ImcGen.exprImCode.get(assignStmt.dst);
        ImcExpr srcExpr = ImcGen.exprImCode.get(assignStmt.src);
        ImcGen.stmtImCode.put(assignStmt, new ImcMOVE(dstExpr, srcExpr));
        return null;
    }

    @Override
    public Object visit(AbsIfStmt ifStmt, Stack<Frame> visArg){
        Vector<ImcStmt> imcIfVector = new Vector<>();
        Label labelTrue = new Label();
        Label labelEnd = new Label();

        ifStmt.cond.accept(this, visArg);
        ImcExpr condition = ImcGen.exprImCode.get(ifStmt.cond);
        ImcSTMTS thenStmts = (ImcSTMTS) ifStmt.thenStmts.accept(this, visArg);

        if (ifStmt.elseStmts.numStmts() == 0) {
            imcIfVector.add(new ImcCJUMP(condition, labelTrue, labelEnd));
            imcIfVector.add(new ImcLABEL(labelTrue));
            imcIfVector.add(thenStmts);
            imcIfVector.add(new ImcLABEL(labelEnd));
        } else {
            Label labelFalse = new Label();
            ImcSTMTS elseStmts = (ImcSTMTS) ifStmt.elseStmts.accept(this, visArg);

            imcIfVector.add(new ImcCJUMP(condition, labelTrue, labelFalse));
            imcIfVector.add(new ImcLABEL(labelTrue));
            imcIfVector.add(thenStmts);
            imcIfVector.add(new ImcJUMP(labelEnd));
            imcIfVector.add(new ImcLABEL(labelFalse));
            imcIfVector.add(elseStmts);
            imcIfVector.add(new ImcLABEL(labelEnd));
        }
        ImcGen.stmtImCode.put(ifStmt, new ImcSTMTS(imcIfVector));
        return null;
    }

    @Override
    public Object visit(AbsWhileStmt whileStmt, Stack<Frame> visArg){
        Vector<ImcStmt> imcWhileVector = new Vector<>();
        Label labelStart = new Label();
        Label labelTrue = new Label();
        Label labelEnd = new Label();
        whileStmt.cond.accept(this, visArg);
        ImcExpr condition = ImcGen.exprImCode.get(whileStmt.cond);
        ImcSTMTS stmts = (ImcSTMTS) whileStmt.stmts.accept(this, visArg);

        imcWhileVector.add(new ImcLABEL(labelStart));
        imcWhileVector.add(new ImcCJUMP(condition, labelTrue, labelEnd));
        imcWhileVector.add(new ImcLABEL(labelTrue));
        imcWhileVector.add(stmts);
        imcWhileVector.add(new ImcLABEL(labelEnd));
        ImcGen.stmtImCode.put(whileStmt, new ImcSTMTS(imcWhileVector));
        return null;
    }
}
