/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.*;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;

/**
 * @author sliva
 */
public class ExprCanonizer implements ImcVisitor<ImcExpr, Vector<ImcStmt>> {

    private StmtCanonizer stmtCanonizer;
    private ChunkGenerator chunkGenerator;

    public ExprCanonizer(ChunkGenerator chunkGenerator){
        this.chunkGenerator = chunkGenerator;
    }

    public void setStmtCanonizer(StmtCanonizer stmtCanonizer) {
        this.stmtCanonizer = stmtCanonizer;
    }

    @Override
    public ImcExpr visit(ImcSEXPR expr, Vector<ImcStmt> visArg){
        Vector<ImcStmt> stmts = expr.stmt.accept(stmtCanonizer, visArg);
        visArg.addAll(stmts);
        return expr.expr.accept(this, visArg);
    }

    @Override
    public ImcExpr visit(ImcBINOP expr, Vector<ImcStmt> visArg){
        ImcExpr fstExpr = expr.fstExpr.accept(this, visArg);
        ImcTEMP fstTemp = new ImcTEMP(new Temp());
        visArg.add(new ImcMOVE(fstTemp, fstExpr));
        ImcExpr sndExpr = expr.sndExpr.accept(this, visArg);
        ImcTEMP sndTemp = new ImcTEMP(new Temp());
        visArg.add(new ImcMOVE(sndTemp, sndExpr));
        return new ImcBINOP(expr.oper, fstTemp, sndTemp);
    }

    @Override
    public ImcExpr visit(ImcUNOP expr, Vector<ImcStmt> visArg){
        ImcExpr subExpr = expr.subExpr;
        ImcTEMP subTemp = new ImcTEMP(new Temp());
        visArg.add(new ImcMOVE(subTemp, subExpr));
        return new ImcUNOP(expr.oper, subTemp);
    }

    @Override
    public ImcExpr visit(ImcCALL call, Vector<ImcStmt> visArg){
        Vector<ImcExpr> newArgs = new Vector<>();
        for (ImcExpr expr: call.args()){
            ImcExpr argExpr = expr.accept(this, visArg);
            ImcTEMP temp = new ImcTEMP(new Temp());
            visArg.add(new ImcMOVE(temp, argExpr));
            newArgs.add(temp);
        }
        return new ImcCALL(call.label, newArgs);
    }

    @Override
    public ImcExpr visit(ImcMEM mem, Vector<ImcStmt> visArg){
        ImcExpr expr = mem.addr.accept(this, visArg);
        if (!(expr instanceof ImcTEMP)) {
            ImcTEMP temp = new ImcTEMP(new Temp());
            visArg.add(new ImcMOVE(temp, expr));
            return new ImcMEM(temp);
        }
        return new ImcMEM(expr);
    }

    @Override
    public ImcExpr visit(ImcCONST constant, Vector<ImcStmt> visArg){
        return constant;
    }

    @Override
    public ImcExpr visit(ImcNAME name, Vector<ImcStmt> visArg){
        return name;
    }

    @Override
    public ImcExpr visit(ImcTEMP temp, Vector<ImcStmt> visArg){
        return temp;
    }
}
