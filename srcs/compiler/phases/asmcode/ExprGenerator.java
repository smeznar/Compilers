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
    public Temp visit(ImcUNOP expr, Vector<AsmInstr> visArg){
        Temp subResult = expr.subExpr.accept(this, visArg);
        Temp result = new Temp();
        Vector<Temp> uses = new Vector<>();
        uses.add(subResult);
        Vector<Temp> defines = new Vector<>();
        defines.add(result);
        if (expr.oper == ImcUNOP.Oper.NOT){
            visArg.add(new AsmOPER("CMP `d0,1,`s0", uses, defines, null));
        } else {
            visArg.add(new AsmOPER("NEG 'd0,0,`s0", uses, defines, null));
        }
        return result;
    }

    @Override
    public Temp visit(ImcBINOP expr, Vector<AsmInstr> visArg){
        Temp fst = expr.fstExpr.accept(this, visArg);
        Temp snd = expr.sndExpr.accept(this, visArg);
        Temp result = new Temp();
        Vector<Temp> uses = new Vector<>();
        Vector<Temp> defines = new Vector<>();
        uses.add(fst);
        uses.add(snd);
        defines.add(result);
        switch (expr.oper){
            case ADD: {
                visArg.add(new AsmOPER("ADD `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case SUB: {
                visArg.add(new AsmOPER("SUB `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case MUL: {
                visArg.add(new AsmOPER("MUL `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case DIV: {
                break;
            }
            case MOD: {
                break;
            }
            case AND: {
                visArg.add(new AsmOPER("AND `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case IOR: {
                visArg.add(new AsmOPER("OR `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case XOR: {
                visArg.add(new AsmOPER("XOR `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case EQU: {
                visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defines, null));
                Vector<Temp> defMul = new Vector<>();
                Vector<Temp> usesMul = new Vector<>();
                defMul.add(result);
                usesMul.add(result);
                usesMul.add(result);
                visArg.add(new AsmOPER("MUL `d0,`s0,`s1", usesMul, defMul, null));
                Vector<Temp> defSub = new Vector<>();
                Vector<Temp> usesSub = new Vector<>();
                defSub.add(result);
                usesSub.add(result);
                visArg.add(new AsmOPER("SUB `d0,1,`s0", usesSub, defSub, null));
                break;
            }
            case NEQ: {
                visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defines, null));
                Vector<Temp> defMul = new Vector<>();
                Vector<Temp> usesMul = new Vector<>();
                defMul.add(result);
                usesMul.add(result);
                usesMul.add(result);
                visArg.add(new AsmOPER("MUL `d0,`s0,`s1", usesMul, defMul, null));
                break;
            }
            case LTH: {
                break;
            }
            case LEQ: {
                break;
            }
            case GTH: {
                break;
            }
            case GEQ: {
                break;
            }
        }
        return result;
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
