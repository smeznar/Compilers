/**
 * @author sliva
 */
package compiler.phases.livean;

import java.util.*;

import compiler.common.report.Report;
import compiler.data.asmcode.*;
import compiler.data.layout.*;
import compiler.phases.*;
import compiler.phases.asmcode.*;

/**
 * @author sliva
 */
public class LiveAn extends Phase {

	public LiveAn() {
		super("livean");
	}

	public void chunkLiveness(Code code) {
		boolean isDifferent;
		do {
			isDifferent = false;
			for (int i=code.instrs.size()-1; i>=0; i--){
				AsmInstr instr = code.instrs.elementAt(i);
				HashSet<Temp> newIn = new HashSet<>(instr.uses());
				newIn.addAll(instr.out());
				newIn.removeAll(instr.defs());
				HashSet<Temp> newOut = new HashSet<>();
				if (instr.jumps().size() == 0){
					newOut.addAll(code.instrs.elementAt(i+1).in());
				} else {
					for (Label jmpLabel:instr.jumps()){
						newOut.addAll(findSuccesorsIns(code, jmpLabel));
					}
				}
				if (!(newIn.equals(instr.in()) && newOut.equals(instr.out()))){
					isDifferent = true;
					instr.addInTemps(newIn);
					instr.addOutTemp(newOut);
				}
			}
		} while (isDifferent);
	}

	private HashSet<Temp> findSuccesorsIns(Code code, Label label){
		if (label.equals(code.exitLabel)){
			return new HashSet<>();
		} else {
			for (AsmInstr instr : code.instrs){
				if (instr instanceof AsmLABEL && instr.toString().equals(label.name)){
					return instr.in();
				}
			}
		}
		throw new Report.Error("There is no Label" + label.name +" in the code chunk");
	}

	public void chunksLiveness() {
		for (Code code : AsmGen.codes) {
			chunkLiveness(code);
		}
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			{
				logger.begElement("code");
				logger.addAttribute("entrylabel", code.entryLabel.name);
				logger.addAttribute("exitlabel", code.exitLabel.name);
				logger.addAttribute("tempsize", Long.toString(code.tempSize));
				code.frame.log(logger);
				logger.begElement("instructions");
				for (AsmInstr instr : code.instrs) {
					logger.begElement("instruction");
					logger.addAttribute("code", instr.toString());
					logger.begElement("temps");
					logger.addAttribute("name", "use");
					for (Temp temp : instr.uses()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.begElement("temps");
					logger.addAttribute("name", "def");
					for (Temp temp : instr.defs()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.begElement("temps");
					logger.addAttribute("name", "in");
					for (Temp temp : instr.in()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.begElement("temps");
					logger.addAttribute("name", "out");
					for (Temp temp : instr.out()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.endElement();
				}
				logger.endElement();
				logger.endElement();
			}
		}
	}

}
