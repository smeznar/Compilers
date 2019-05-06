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
                visArg.add(new AsmOPER("DIV `d0,`s0,`s1", uses, defines, null));
                break;
            }
            case MOD: {
                visArg.add(new AsmOPER("DIV `d0,`s0,`s1", uses, defines, null));
                Vector<Temp> usesMul = new Vector<>();
                usesMul.add(result);
                usesMul.add(snd);
                visArg.add(new AsmOPER("MUL `d0,`s0,`s1", usesMul, defines, null));
                Vector<Temp> usesSub = new Vector<>();
                usesSub.add(fst);
                usesSub.add(result);
                visArg.add(new AsmOPER("SUB `d0,`s0,`s1", usesSub, defines, null));
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
                visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defines, null));
                Vector<Temp> usesCmp = new Vector<>();
                usesCmp.add(result);
                visArg.add(new AsmOPER("CMP `d0,`s0,-1", usesCmp, defines, null));
                visArg.add(new AsmOPER("SUB `d0,1,`s0", usesCmp, defines, null));
                break;
            }
            case LEQ: {
                visArg.add(new AsmOPER("CMP `d0,`s1,`s0", uses, defines, null));
                Vector<Temp> usesCmp = new Vector<>();
                usesCmp.add(result);
                visArg.add(new AsmOPER("CMP `d0,`s0,-1", usesCmp, defines, null));
                break;
            }
            case GTH: {
                visArg.add(new AsmOPER("CMP `d0,`s1,`s0", uses, defines, null));
                Vector<Temp> usesCmp = new Vector<>();
                usesCmp.add(result);
                visArg.add(new AsmOPER("CMP `d0,`s0,-1", usesCmp, defines, null));
                visArg.add(new AsmOPER("SUB `d0,1,`s0", usesCmp, defines, null));
                break;
            }
            case GEQ: {
                visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defines, null));
                Vector<Temp> usesCmp = new Vector<>();
                usesCmp.add(result);
                visArg.add(new AsmOPER("CMP `d0,`s0,-1", usesCmp, defines, null));
                break;
            }
        }
        return result;
    }

    @Override
    public Temp visit(ImcCALL call, Vector<AsmInstr> visArg){
        int i = 0;
        for (ImcExpr expr : call.args()){
            // TODO: Maybe do something with static link?
            Temp arg = expr.accept(this, visArg);
            Vector<Temp> uses = new Vector<>();
            uses.add(arg);
            visArg.add(new AsmOPER("STO `s0,$254," + i, uses, null, null));
            i += 8;
        }
        Temp result = new Temp();
        Vector<Temp> uses = new Vector<>();
        uses.add(result);
        visArg.add(new AsmOPER("PUSHJ `s0,"+call.label.name, uses, null, null));
        // TODO: Change the return value?
        return result;
    }

    @Override
    public Temp visit(ImcMEM mem, Vector<AsmInstr> visArg){
        Temp addr = mem.addr.accept(this, visArg);
        Temp result = new Temp();
        Vector<Temp> uses = new Vector<>();
        uses.add(addr);
        Vector<Temp> defines = new Vector<>();
        defines.add(result);
        visArg.add(new AsmOPER("LDO `d0,`s0,0", uses, defines, null));
        return result;
    }

    @Override
    public Temp visit(ImcTEMP temp, Vector<AsmInstr> visArg){
        return temp.temp;
    }

    //////// Can be handled in StmtGenerator also

    @Override
    public Temp visit(ImcNAME name, Vector<AsmInstr> visArg){
        Temp result = new Temp();
        Vector<Temp> defines = new Vector<>();
        defines.add(result);
        visArg.add(new AsmOPER("LDA `d0," + name.label.name, null, defines, null));
        return result;
    }

    @Override
    public Temp visit(ImcCONST constant, Vector<AsmInstr> visArg){
        Temp result = new Temp();
        Vector<Temp> uses = new Vector<>();
        uses.add(result);
        Vector<Temp> defines = new Vector<>();
        defines.add(result);
        // Todo: Popravi ukaze
        visArg.add(new AsmOPER("SETL `d0,...", null, defines, null));
        visArg.add(new AsmOPER("INCML `d0,...", uses, defines, null));
        visArg.add(new AsmOPER("INCMH `d0,...", uses, defines, null));
        visArg.add(new AsmOPER("INCH `d0,...", uses, defines, null));

        return result;
    }

    //////////////////////////////////////////////

    @Override
    public Temp visit(ImcSEXPR expr, Vector<AsmInstr> visArg){
        throw new Report.Error("[AsmCode] Cannot have ImcSEXPR in AsmCode phase.");
    }
}
