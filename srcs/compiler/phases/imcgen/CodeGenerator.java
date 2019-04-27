/**
 * @author sliva
 */
package compiler.phases.imcgen;

import java.util.*;

import compiler.common.report.Report;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.imcode.*;
import compiler.data.layout.*;
import compiler.data.type.SemCharType;
import compiler.data.type.SemRecType;
import compiler.data.type.SemType;
import compiler.phases.frames.*;
import compiler.phases.seman.SemAn;
import compiler.phases.seman.SymbTable;
import compiler.phases.seman.TypeResolver;

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
        visArg.push(Frames.frames.get(fun));
        super.visit(fun, visArg);
        visArg.pop();
        return null;
    }


    @Override
    public Object visit(AbsAtomExpr atomExpr, Stack<Frame> visArg){
        switch (atomExpr.type){
            case PTR:
            case VOID: {
                ImcGen.exprImCode.put(atomExpr, new ImcCONST(0));
                return null;
            }
            case BOOL: {
                ImcExpr atom;
                atom = atomExpr.expr.equals("false") ? new ImcCONST(0) : new ImcCONST(1);
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
                ImcGen.exprImCode.put(atomExpr, new ImcCONST((long)character));
                return null;
            }
            case STR: {
                AbsAccess access = Frames.strings.get(atomExpr);
                ImcGen.exprImCode.put(atomExpr, new ImcMEM(new ImcNAME(access.label)));
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
        arrExpr.array.accept(this, visArg);
        arrExpr.index.accept(this, visArg);
        if (!SemAn.isAddr.get(arrExpr.array)){
            throw new Report.Error(arrExpr, "[ImcGen] Array expression should have an address on the left side.");
        }
        ImcMEM exprLeft = (ImcMEM) ImcGen.exprImCode.get(arrExpr.array);
        ImcExpr addr1 = exprLeft.addr;
        ImcExpr index = ImcGen.exprImCode.get(arrExpr.index);
        ImcCONST arrSize = new ImcCONST(SemAn.ofType.get(arrExpr).size());
        ImcBINOP indexVal = new ImcBINOP(ImcBINOP.Oper.MUL, index, arrSize);
        ImcBINOP arrAddr = new ImcBINOP(ImcBINOP.Oper.ADD, addr1, indexVal);
        ImcGen.exprImCode.put(arrExpr, new ImcMEM(arrAddr));
        return null;
    }

    @Override
    public Object visit(AbsRecExpr expr, Stack<Frame> visArg){
        expr.record.accept(this, visArg);
        if (!SemAn.isAddr.get(expr.record)){
            throw new Report.Error(expr, "[ImcGen] Record should have an address on the left side.");
        }
        ImcMEM rec = (ImcMEM) ImcGen.exprImCode.get(expr.record);
        ImcExpr recAddr = rec.addr;
        // Ask if it is ok
        RelAccess access = getComponentAccess(expr.record, expr.comp);
        ImcBINOP compAddr = new ImcBINOP(ImcBINOP.Oper.ADD, recAddr, new ImcCONST(access.offset));
        ImcGen.exprImCode.put(expr, new ImcMEM(compAddr));
        return compAddr;
    }

    private RelAccess getComponentAccess(AbsExpr record, AbsVarName comp){
        AbsVarDecl decl = null;
        try {
            SemRecType type = (SemRecType) SemAn.ofType.get(record).actualType();
            SymbTable table = TypeResolver.symbTables.get(type);
            decl = (AbsVarDecl) table.fnd(comp.name);
        } catch (SymbTable.CannotFndNameException e) {
            throw new Report.Error(comp, "Cannot find name of component.");
        } catch (Exception e){
            throw new Report.Error(record, "Cannot find record or its components.");
        }
        return (RelAccess) Frames.accesses.get(decl);
    }

    @Override
    public Object visit(AbsUnExpr expr, Stack<Frame> visArg){
        expr.subExpr.accept(this, visArg);
        switch (expr.oper){
            case ADD: {
                ImcExpr subExpr = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, subExpr);
                return null;
            }
            case SUB: {
                ImcExpr expr1 = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, new ImcUNOP(ImcUNOP.Oper.NEG, expr1));
                return null;
            }
            case NOT: {
                ImcExpr expr1 = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, new ImcUNOP(ImcUNOP.Oper.NOT, expr1));
                return null;
            }
            case DATA: {
                ImcExpr subExpr = ImcGen.exprImCode.get(expr.subExpr);
                ImcGen.exprImCode.put(expr, new ImcMEM(subExpr));
                return null;
            }
            case ADDR: {
                if (SemAn.isAddr.get(expr.subExpr)){
                    ImcMEM subExpr = (ImcMEM) ImcGen.exprImCode.get(expr.subExpr);
                    ImcGen.exprImCode.put(expr, subExpr.addr);
                    return null;
                } else {
                    throw new Report.Error(expr, "[ImcGen] The expression does not have an address.");
                }
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
            case DIV: oper = ImcBINOP.Oper.DIV; break;
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
        }
        ImcBINOP binop = new ImcBINOP(oper, expr1, expr2);
        ImcGen.exprImCode.put(expr, binop);
        return null;
    }

    @Override
    public Object visit(AbsNewExpr newExpr, Stack<Frame> visArg){
        SemType type = SemAn.isType.get(newExpr.type);
        Vector<ImcExpr> args = new Vector<>();
        Frame thisFrame = visArg.peek();
        ImcExpr sl = new ImcTEMP(visArg.peek().FP);
        for (int i = 1; i<thisFrame.depth; i++){
            sl = new ImcMEM(sl);
        }
        args.add(sl);
        args.add(new ImcCONST(type.size()));
        ImcCALL call = new ImcCALL(new Label("new"), args);
        ImcGen.exprImCode.put(newExpr, call);
        return null;
    }

    @Override
    public Object visit(AbsDelExpr delExpr, Stack<Frame> visArg){
        Vector<ImcExpr> args = new Vector<>();
        Frame thisFrame = visArg.peek();
        ImcExpr sl = new ImcTEMP(visArg.peek().FP);
        for (int i = 1; i<thisFrame.depth; i++){
            sl = new ImcMEM(sl);
        }
        args.add(sl);
        delExpr.expr.accept(this, visArg);
        args.add(ImcGen.exprImCode.get(delExpr.expr));
        ImcGen.exprImCode.put(delExpr, new ImcCALL(new Label("del"), args));
        return null;
    }

    @Override
    public Object visit(AbsFunName funName, Stack<Frame> visArg){
        Vector<ImcExpr> args = new Vector<>();
        Frame calledFrame = Frames.frames.get((AbsFunDecl) SemAn.declaredAt.get(funName));
        Frame thisFrame = visArg.peek();
        ImcExpr sl = new ImcTEMP(visArg.peek().FP);
        for (int i = calledFrame.depth; i<thisFrame.depth; i++){
            sl = new ImcMEM(sl);
        }
        args.add(sl);
        for (AbsExpr expr : funName.args.args()){
            expr.accept(this, visArg);
            args.add(ImcGen.exprImCode.get(expr));
        }
        AbsFunDecl funDecl = (AbsFunDecl) SemAn.declaredAt.get(funName);
        Frame frame = Frames.frames.get(funDecl);
        ImcGen.exprImCode.put(funName, new ImcCALL(frame.label, args));
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
        return new ImcSTMTS(imcStmts);
    }

    @Override
    public Object visit(AbsCastExpr castExpr, Stack<Frame> visArg){
        SemType type = SemAn.isType.get(castExpr.type).actualType();
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
        if (!SemAn.isAddr.get(assignStmt.dst)){
            throw new Report.Error(assignStmt, "[ImcGen] Destination expression should be an address.");
        }
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
        imcWhileVector.add(new ImcJUMP(labelStart));
        imcWhileVector.add(new ImcLABEL(labelEnd));
        ImcGen.stmtImCode.put(whileStmt, new ImcSTMTS(imcWhileVector));
        return null;
    }
}
