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
}
