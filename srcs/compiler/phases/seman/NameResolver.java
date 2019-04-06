/**
 * @author sliva
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;

/**
 * Name resolving: the result is stored in {@link SemAn#declaredAt}.
 * 
 * @author sliva
 */
public class NameResolver extends AbsFullVisitor<Object, Object> {

	/** Symbol table. */
	private final SymbTable symbTable = new SymbTable();

	private static final int TYP_DECLARATION_PH = 0;
	private static final int TYP_ELABOR_PH = 1;
	private static final int VAR_FUN_TYP_CHECK_PH = 2;
	private static final int VAR_FUN_DECLARATION_PH = 3;
	private static final int FUNCTION_PH = 4;

	@Override
	public Object visit(AbsSource source, Object visArg) {
		for (int i=0; i<5; i++){
			source.decls.accept(this, i);
		}
		return null;
	}

	@Override
	public Object visit(AbsTypDecl decl, Object visArg){
		try {
			if ((int) visArg == TYP_DECLARATION_PH){
				symbTable.ins(decl.name, decl);
			} else if ((int) visArg == TYP_ELABOR_PH){
				decl.type.accept(this, visArg);
			} else if ((int) visArg == FUNCTION_PH){
				decl.type.accept(this, visArg);
			}
			return null;
		} catch (SymbTable.CannotInsNameException e) {
			throw createInsertionError(decl);
		} catch (Exception e){
			throw new Report.InternalError();
		}
	}

	@Override
	public Object visit(AbsTypName typName, Object visArg){
		try {
			if ((int) visArg == TYP_ELABOR_PH) {
				AbsDecl declLocation = symbTable.fnd(typName.name);
				SemAn.declaredAt.put(typName, declLocation);
			} else if ((int) visArg==VAR_FUN_TYP_CHECK_PH){
				AbsDecl declLocation = symbTable.fnd(typName.name);
				SemAn.declaredAt.put(typName, declLocation);
			} else if ((int) visArg==FUNCTION_PH){
				AbsDecl declLocation = symbTable.fnd(typName.name);
				SemAn.declaredAt.put(typName, declLocation);
			}
		} catch (SymbTable.CannotFndNameException e){
			throw createSearchError(typName);
		} catch (Exception e){
			throw new Report.InternalError();
		}
		return null;
	}

	@Override
	public Object visit(AbsArrType arr, Object visArg){
		try {
			if ((int) visArg==TYP_ELABOR_PH){
				arr.elemType.accept(this, visArg);
			} else if ((int) visArg==VAR_FUN_TYP_CHECK_PH){
				arr.elemType.accept(this, visArg);
			} else if ((int) visArg==FUNCTION_PH){
				arr.len.accept(this, visArg);
			}
		} catch (Exception e){
			throw new Report.InternalError();
		}
		return null;
	}

	@Override
	public Object visit(AbsVarDecl decl, Object visArg){
		try {
			if ((int) visArg == VAR_FUN_TYP_CHECK_PH){
				decl.type.accept(this, visArg);
			} else if ((int) visArg==VAR_FUN_DECLARATION_PH){
				symbTable.ins(decl.name, decl);
			} else if ((int) visArg==FUNCTION_PH){
				decl.type.accept(this, visArg);
			}
		} catch (SymbTable.CannotInsNameException e){
			throw createInsertionError(decl);
		} catch (Exception e){
			throw new Report.InternalError();
		}
		return null;
	}

	@Override
	public Object visit(AbsFunDecl decl, Object visArg){
		try {
			if ((int) visArg==VAR_FUN_TYP_CHECK_PH){
				decl.parDecls.accept(this, visArg);
				decl.type.accept(this, visArg);
			} else if ((int) visArg==VAR_FUN_DECLARATION_PH){
				symbTable.ins(decl.name, decl);
			} else if ((int) visArg==FUNCTION_PH){
				decl.type.accept(this, visArg);
				symbTable.newScope();
				decl.parDecls.accept(this, visArg);
				symbTable.oldScope();
			}
		} catch (SymbTable.CannotInsNameException e){
			throw createInsertionError(decl);
		} catch (Exception e){
			throw new Report.InternalError();
		}
		return null;
	}

	@Override
	public Object visit(AbsFunDef decl, Object visArg){
		try {
			if ((int) visArg==VAR_FUN_TYP_CHECK_PH){
				decl.parDecls.accept(this, visArg);
				decl.type.accept(this, visArg);
			} else if ((int) visArg==VAR_FUN_DECLARATION_PH){
				symbTable.ins(decl.name, decl);
			} else if ((int) visArg==FUNCTION_PH){
				decl.type.accept(this, visArg);
				symbTable.newScope();
				decl.parDecls.accept(this, visArg);
				decl.value.accept(this, visArg);
				symbTable.oldScope();
			}
		} catch (SymbTable.CannotInsNameException e){
			throw createInsertionError(decl);
		} catch (Exception e){
			throw new Report.InternalError();
		}
		return null;
	}

	@Override
	public Object visit(AbsParDecl par, Object visArg){
		try {
			if ((int) visArg==VAR_FUN_TYP_CHECK_PH){
				par.type.accept(this, visArg);
			} else if ((int) visArg==FUNCTION_PH){
				par.type.accept(this, visArg);
				symbTable.ins(par.name, par);
			}
		} catch (SymbTable.CannotInsNameException e){
			throw createInsertionError(par);
		} catch (Exception e){
			throw new Report.InternalError();
		}
		return null;
	}

	@Override
	public Object visit(AbsBlockExpr blockExpr, Object visArg){
		symbTable.newScope();
		for (int i=0; i<5; i++) {
			blockExpr.decls.accept(this, i);
		}
		blockExpr.stmts.accept(this, visArg);
		blockExpr.expr.accept(this, visArg);
		symbTable.oldScope();
		return null;
	}

	@Override
	public Object visit(AbsVarName varName, Object visArg){
		try {
			AbsDecl decl = symbTable.fnd(varName.name);
			SemAn.declaredAt.put(varName, decl);
		} catch (SymbTable.CannotFndNameException e){
			throw createSearchError(varName);
		}
		return null;
	}

	@Override
	public Object visit(AbsFunName funName, Object visArg){
		try {
			AbsDecl decl = symbTable.fnd(funName.name);
			SemAn.declaredAt.put(funName, decl);
			funName.args.accept(this, visArg);
		} catch (SymbTable.CannotFndNameException e){
			throw createSearchError(funName);
		}
		return null;
	}

	@Override
	public Object visit(AbsRecExpr recExpr, Object visArg){
		recExpr.record.accept(this, visArg);
		return null;
	}

	private Report.Error createInsertionError(AbsTree node){
		return new Report.Error(node, "Only one identifier with the same name allowed per scope.");
	}

	private Report.Error createSearchError(AbsTree node){
		return new Report.Error(node, "Identifier with this name could not be found in this scope or higher..");
	}
}
