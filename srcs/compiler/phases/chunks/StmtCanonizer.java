/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.*;
import compiler.common.report.*;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;

/**
 * @author sliva
 */
public class StmtCanonizer implements ImcVisitor<Vector<ImcStmt>, Object> {

    private ExprCanonizer exprCanonizer;

    public void setExprCanonizer(ExprCanonizer exprCanonizer) {
        this.exprCanonizer = exprCanonizer;
    }

    @Override
    public Vector<ImcStmt> visit(ImcCJUMP cjump, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        ImcExpr cond = cjump.cond.accept(exprCanonizer, stmts);
        ImcTEMP temp = new ImcTEMP(new Temp());
        stmts.add(new ImcMOVE(temp, cond));
        //stmts.add(new ImcCJUMP(temp, cjump.posLabel, cjump.negLabel));
        Label falseLabel = new Label();
        stmts.add(new ImcCJUMP(temp, cjump.posLabel, falseLabel));
        stmts.add(new ImcLABEL(falseLabel));
        stmts.add(new ImcJUMP(cjump.negLabel));
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcJUMP jump, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        stmts.add(jump);
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcMOVE move, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        ImcExpr dst = move.dst.accept(exprCanonizer, stmts);
        ImcExpr src = move.src.accept(exprCanonizer, stmts);
        stmts.add(new ImcMOVE(dst, src));
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcESTMT estmt, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        ImcExpr expr = estmt.expr.accept(exprCanonizer, stmts);
        stmts.add(new ImcESTMT(expr));
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcSTMTS stmts, Object visArg){
        Vector<ImcStmt> allStmts = new Vector<>();
        for (ImcStmt stmt : stmts.stmts()){
            allStmts.addAll(stmt.accept(this, visArg));
        }
        return allStmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcLABEL label, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        stmts.add(label);
        return stmts;
    }
}
