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
        exprCanonizer = new ExprCanonizer(this);
        stmtCanonizer = new StmtCanonizer(this);
        exprCanonizer.setStmtCanonizer(stmtCanonizer);
        stmtCanonizer.setExprCanonizer(exprCanonizer);
    }

    @Override
    public Object visit(AbsFunDef funDef, Object visArg){
        Vector<ImcStmt> stmts = new Vector<>();
        Label startLabel = new Label();
        stmts.add(new ImcLABEL(startLabel));
        ImcExpr expr = ImcGen.exprImCode.get(funDef.value);
        expr.accept(exprCanonizer, stmts);

        return null;
    }

}
