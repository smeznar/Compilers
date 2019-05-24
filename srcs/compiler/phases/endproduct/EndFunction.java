package compiler.phases.endproduct;

import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.AsmLABEL;
import compiler.data.asmcode.Code;

import java.util.Vector;

public class EndFunction {

    Code code;
    public Vector<String> instructions;

    EndFunction(Code code){
        this.code = code;
        instructions = new Vector<>();
        transformFunction();
    }

    private EndFunction(){
        instructions = new Vector<>();
    }

    private void transformFunction(){
        addPrologue();
        String instrStr = "\t\t";
        for (AsmInstr instr : code.instrs){
            if (instr instanceof AsmLABEL){
                instrStr = ((AsmLABEL) instr).label.name + "\t";
            }
            else {
                instrStr += instr.toString(code.regs);
                instructions.add(instrStr);
                instrStr = "\t\t";
            }
        }
        addEpilogue();
    }

    private void addPrologue(){

    }

    private void addEpilogue(){

    }

    public static Vector<EndFunction> getSystemFunctions(){
        Vector<EndFunction> fns = new Vector<>();
        fns.add(addBootstrapStart());
        fns.add(addNewFun());
        fns.add(addDelFun());
        fns.add(putStrFun());
        fns.add(putIntFun());
        return fns;
    }

    public static EndFunction addBootstrapStart(){
        EndFunction fn = new EndFunction();
        Vector<String> instrs = fn.instructions;
        return fn;
    }

    public static EndFunction addNewFun(){
        EndFunction fn = new EndFunction();
        Vector<String> instrs = fn.instructions;
        return fn;
    }

    public static EndFunction addDelFun(){
        EndFunction fn = new EndFunction();
        Vector<String> instrs = fn.instructions;
        return fn;
    }

    public static EndFunction putStrFun(){
        EndFunction fn = new EndFunction();
        Vector<String> instrs = fn.instructions;
        return fn;
    }

    public static EndFunction putIntFun(){
        EndFunction fn = new EndFunction();
        Vector<String> instrs = fn.instructions;
        return fn;
    }
}
