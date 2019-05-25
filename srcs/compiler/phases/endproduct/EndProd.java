package compiler.phases.endproduct;

import compiler.data.asmcode.Code;
import compiler.data.chunk.DataChunk;
import compiler.phases.asmcode.AsmGen;
import compiler.phases.chunks.Chunks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Vector;

public class EndProd {

    public EndProd() {
        functions = new Vector<>();
    }

    private Vector<EndFunction> functions;

    public void finishCode(){
        functions.addAll(EndFunction.getSystemFunctions());
        for (Code c : AsmGen.codes){
            functions.add(new EndFunction(c));
        }
    }

    public void log(){
        String destinationFileName = compiler.Main.cmdLineArgValue("--dst-file-name");
        try{
            FileWriter fwStream = new FileWriter(destinationFileName);
            BufferedWriter writer = new BufferedWriter(fwStream);
            writeCode(writer);
            writer.close();
            fwStream.close();
        }catch(Exception e){System.out.println(e);}
    }

    private void writeCode(BufferedWriter writer){
        writeDataChunks(writer);
        writeCodeChunks(writer);
    }

    private void writeDataChunks(BufferedWriter writer){
        try {
            writer.write("sP\tGREG");
            writer.newLine();
            writer.write("fP\tGREG");
            writer.newLine();
            writer.write("\t\tLOC\tData_Segment");
            writer.newLine();
            writer.write("\t\tGREG @");
            writer.newLine();
            writer.write("\t\tBYTE 0,0,0,0,0,0,0,0");
            writer.newLine();
            writer.write("numStr\tBYTE 0");
            writer.newLine();
            for (DataChunk chunk : Chunks.dataChunks){
                if (chunk.init != null){
                    writer.write(chunk.label.name);
                    writer.write('\t');
                    writer.write("BYTE\t");
                    writer.write(chunk.init);
                    writer.write(",0");
                    writer.newLine();
                } else {
                    writer.write(chunk.label.name);
                    writer.write('\t');
                    writer.write("OCTA\t");
                    writer.write("0");
                    writer.newLine();
                }
            }
            writer.newLine();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void writeCodeChunks(BufferedWriter writer){
        // TODO: Something
        try {
            writer.write("\t\tLOC\t#80000");
            writer.newLine();
            writer.newLine();
            for (EndFunction fn : functions){
                for (String str : fn.instructions){
                    writer.write(str);
                    writer.newLine();
                }
                writer.newLine();
            }
            writer.newLine();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
