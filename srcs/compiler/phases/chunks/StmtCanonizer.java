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
        return null;
    }

    @Override
    public Vector<ImcStmt> visit(ImcJUMP jump, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        stmts.add(jump);
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcLABEL label, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        stmts.add(label);
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcMOVE move, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        move.dst.accept(exprCanonizer, stmts);
        move.src.accept(exprCanonizer, stmts);
        stmts.add(move);
        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcESTMT estmt, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        estmt.expr.accept(exprCanonizer, stmts);
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
}
