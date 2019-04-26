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
    private ChunkGenerator chunkGenerator;

    public StmtCanonizer(ChunkGenerator chunkGenerator){
        this.chunkGenerator = chunkGenerator;
    }

    public void setExprCanonizer(ExprCanonizer exprCanonizer) {
        this.exprCanonizer = exprCanonizer;
    }
}
