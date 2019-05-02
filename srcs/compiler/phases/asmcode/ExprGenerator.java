/**
 * @author sliva
 */
package compiler.phases.asmcode;

import java.util.*;

import compiler.common.report.Report;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;
import compiler.data.asmcode.*;

/**
 * @author sliva
 */
public class ExprGenerator implements ImcVisitor<Temp, Vector<AsmInstr>> {

    @Override
    public Temp visit(ImcBINOP expr, Vector<AsmInstr> visArg){
        return null;
    }

    @Override
    public Temp visit(ImcUNOP expr, Vector<AsmInstr> visArg){
        return null;
    }

    @Override
    public Temp visit(ImcCALL call, Vector<AsmInstr> visArg){
        return null;
    }

    @Override
    public Temp visit(ImcMEM mem, Vector<AsmInstr> visArg){
        return null;
    }

    @Override
    public Temp visit(ImcNAME name, Vector<AsmInstr> visArg){
        return null; // cannot happen?
    }

    @Override
    public Temp visit(ImcTEMP temp, Vector<AsmInstr> visArg){
        return temp.temp;
    }

    @Override
    public Temp visit(ImcCONST constant, Vector<AsmInstr> visArg){
        return null; // If it comes to a constant it should be an expression statement.
    }

    @Override
    public Temp visit(ImcSEXPR expr, Vector<AsmInstr> visArg){
        throw new Report.Error("[AsmCode] Cannot have ImcSEXPR in AsmCode phase.");
    }
}
