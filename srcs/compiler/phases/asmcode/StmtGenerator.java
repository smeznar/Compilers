/**
 * @author sliva
 */
package compiler.phases.asmcode;

import java.util.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;
import compiler.data.layout.*;
import compiler.data.asmcode.*;
import compiler.common.report.*;

/**
 * @author sliva
 */
public class StmtGenerator implements ImcVisitor<Vector<AsmInstr>, Object> {

    private ExprGenerator exprGenerator;

    private ExprGenerator getExpressionGenerator(){
        if (exprGenerator == null){
            exprGenerator = new ExprGenerator();
        }
        return exprGenerator;
    }

    @Override
    public Vector<AsmInstr> visit(ImcCJUMP cjump, Object visArg){
        Vector<AsmInstr> instructions = new Vector<>();
        Vector<Temp> uses = new Vector<>();
        Vector<Label> jumps = new Vector<>();
        jumps.add(cjump.posLabel);
        jumps.add(cjump.negLabel);
        uses.add(cjump.cond.accept(getExpressionGenerator(), instructions));
        instructions.add(new AsmOPER("BNZ " + cjump.posLabel.name, uses, null, jumps));
        return instructions;
    }

    @Override
    public Vector<AsmInstr> visit(ImcJUMP jump, Object visArg){
        Vector<AsmInstr> instructions = new Vector<>();
        Vector<Label> jumps = new Vector<>();
        jumps.add(jump.label);
        instructions.add(new AsmOPER("JMP " + jump.label.name, null, null, jumps));
        return instructions;
    }

    @Override
    public Vector<AsmInstr> visit(ImcMOVE move, Object visArg){
        Vector<AsmInstr> instructions = new Vector<>();
        Vector<Temp> uses = new Vector<>();
        Vector<Temp> defines = new Vector<>();
//        if (move.dst instanceof ImcTEMP && move.src instanceof ImcTEMP){
//            uses.add(((ImcTEMP) move.src).temp);
//            defines.add(((ImcTEMP) move.dst).temp);
//            instructions.add(new AsmMOVE("STO `s0,`d0,0", uses, defines)); // Todo: what instruction
//        } else if (move.dst instanceof ImcTEMP){
//            defines.add(((ImcTEMP) move.dst).temp);
//            Temp src = move.src.accept(getExpressionGenerator(), instructions);
//            // TODO: 4je ukazi alpa zdruzit
        if (move.dst instanceof ImcTEMP){
            defines.add(((ImcTEMP) move.dst).temp);
            uses.add(move.src.accept(getExpressionGenerator(), instructions));
            instructions.add(new AsmMOVE("STO `s0,`d0,0", uses, defines)); // Todo: what instruction
        } else if (move.dst instanceof ImcMEM){
            Temp dst = move.dst.accept(getExpressionGenerator(), instructions);
            Temp src = move.src.accept(getExpressionGenerator(), instructions);
            defines.add(dst);
            uses.add(src);
            instructions.add(new AsmOPER("STO `s0,`d0,0", uses, defines, null));
        } else {
            throw new Report.Error("[AsmCode] Destination can only be Mem or Temp.");
        }
        return instructions;
    }

    @Override
    public Vector<AsmInstr> visit(ImcESTMT estmt, Object visArg){
        Vector<AsmInstr> instructions = new Vector<>();
        estmt.expr.accept(getExpressionGenerator(), instructions);
        return instructions;
    }

    @Override
    public Vector<AsmInstr> visit(ImcLABEL label, Object visArg){
        Vector<AsmInstr> instructions = new Vector<>();
        instructions.add(new AsmLABEL(label.label));
        return instructions;
    }

    @Override
    public Vector<AsmInstr> visit(ImcSTMTS stmts, Object visArg){
        throw new Report.Error( "[AsmCode] Cannot have ImcSTMTS in AsmCode phase.");
    }
}
