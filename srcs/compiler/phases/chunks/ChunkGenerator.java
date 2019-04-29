/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.chunk.*;
import compiler.phases.frames.*;
import compiler.phases.imcgen.*;

/**
 * @author sliva
 *
 */
public class ChunkGenerator extends AbsFullVisitor<Object, Object> {

    private ExprCanonizer exprCanonizer;
    private StmtCanonizer stmtCanonizer;

    public ChunkGenerator(){
        exprCanonizer = new ExprCanonizer();
        stmtCanonizer = new StmtCanonizer();
        exprCanonizer.setStmtCanonizer(stmtCanonizer);
        stmtCanonizer.setExprCanonizer(exprCanonizer);
    }

    @Override
    public Object visit(AbsSource source, Object visArg){
        super.visit(source, visArg);
        Interpreter interpreter = new Interpreter(Chunks.dataChunks, Chunks.codeChunks);
        interpreter.run("_main");
        return null;
    }

    @Override
    public Object visit(AbsFunDef funDef, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        Frame frame = Frames.frames.get(funDef);
        Label entryLabel = new Label();
        Label exitLabel = new Label();
        ImcExpr expr = ImcGen.exprImCode.get(funDef.value);
        ImcTEMP temp = new ImcTEMP(frame.RV);

        stmts.add(new ImcLABEL(entryLabel));
        ImcExpr returnedExpr = expr.accept(exprCanonizer, stmts);
        stmts.add(new ImcMOVE(temp, returnedExpr));
        stmts.add(new ImcJUMP(exitLabel));
        Chunks.codeChunks.add(new CodeChunk(frame, stmts, entryLabel, exitLabel));
        return super.visit(funDef, visArg);
    }

    @Override
    public Object visit(AbsVarDecl decl, Object visArg){
        Access access = Frames.accesses.get(decl);
        if (access instanceof AbsAccess){
            Chunks.dataChunks.add(new DataChunk((AbsAccess) access));
        }
        return super.visit(decl, visArg);
    }

    @Override
    public Object visit(AbsAtomExpr expr, Object visArg){
        if (expr.type == AbsAtomExpr.Type.STR){
            AbsAccess access = Frames.strings.get(expr);
            Chunks.dataChunks.add(new DataChunk(access));
        }
        return super.visit(expr, visArg);
    }
}
