/**
 * @author sliva
 */
package compiler.phases.ralloc;

import compiler.data.asmcode.*;
import compiler.phases.*;
import compiler.phases.asmcode.*;

import java.util.Vector;

/**
 * Register allocation phase.
 * 
 * @author sliva
 */
public class RAlloc extends Phase {

	public RAlloc() {
		super("ralloc");
	}

	/**
	 * Computes the mapping of temporary variables to registers for each function.
	 * If necessary, the code of each function is modified.
	 */
	public void tempsToRegs() {
		Vector<Code> codes = AsmGen.codes;
		Vector<Code> modifiedCodes = new Vector<>();
		for (Code c : codes){
			Graph graph = new Graph(c);
			modifiedCodes.add(graph.getModifiedCode());
		}
		AsmGen.codes = modifiedCodes;
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("entrylabel", code.entryLabel.name);
			logger.addAttribute("exitlabel", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString(code.regs));
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}

}
