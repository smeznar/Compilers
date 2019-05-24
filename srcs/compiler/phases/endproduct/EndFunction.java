package compiler.phases.endproduct;

import compiler.data.asmcode.Code;

import java.util.Vector;

public class EndFunction {

    Code code;
    public Vector<String> instructions;

    public EndFunction(Code code){
        this.code = code;
        transformFunction();
    }

    private void transformFunction(){
        addPrologue();

        addEpilogue();
    }

    private void addPrologue(){

    }

    private void addEpilogue(){

    }

    public static Vector<EndFunction> getSystemFunctions(){
        Vector<EndFunction> fns = new Vector<>();
        return fns;
    }
}
