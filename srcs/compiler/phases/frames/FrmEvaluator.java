/**
 * @author sliva
 */
package compiler.phases.frames;

import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.data.layout.*;
import compiler.phases.seman.*;

/**
 * Computing function frames and accesses.
 * 
 * @author sliva
 */
public class FrmEvaluator extends AbsFullVisitor<Object, FrmEvaluator.Context> {

	/**
	 * The context {@link FrmEvaluator} uses while computing function frames and
	 * variable accesses.
	 * 
	 * @author sliva
	 */
	protected abstract class Context {
	}

	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 * 
	 * @author sliva
	 */
	private class FunContext extends Context {
		public int depth = 0;
		public long locsSize = 0;
		public long argsSize = 0;
		public long parsSize = new SemPtrType(new SemVoidType()).size();
	}

	/**
	 * Record context, i.e., used when traversing record definition and computing
	 * record component acceses.
	 *
	 * @author sliva
	 */
	private class RecContext extends Context {
		public long compsSize = 0;
	}

	int numOfLocalfunctions = 0;

	@Override
	public Object visit(AbsSource source, FrmEvaluator.Context visArg){
		FunContext context = new FunContext();
		context.depth = 0;
		return super.visit(source, context);
	}

	@Override
	public Object visit(AbsTypDecl decl, Context visArg){
		return super.visit(decl, new RecContext());
	}

	@Override
	public Object visit(AbsVarDecl decl, FrmEvaluator.Context visArg){
		SemType type = SemAn.isType.get(decl.type);
		FunContext context = ((FunContext) visArg);
		if (context.depth == 0){
			Frames.accesses.put(decl, new AbsAccess(type.size(), new Label(decl.name), null));
			return null;
		}
		context.locsSize += type.size();
		Frames.accesses.put(decl, new RelAccess(type.size(), -context.locsSize, context.depth));
		return super.visit(decl, new RecContext());
	}

	@Override
	public Object visit(AbsFunDef decl, FrmEvaluator.Context visArg){
		FrmEvaluator.FunContext context = new FrmEvaluator.FunContext();
		context.depth = ((FunContext) visArg).depth + 1;
		super.visit(decl, context);
		/*if (context.argsSize > 0){
			context.argsSize += new SemPtrType(new SemVoidType()).size();
		}*/
		context.argsSize += new SemPtrType(new SemVoidType()).size();
		String label;
		if (context.depth == 1){
			label = decl.name;
		} else {
			label = "L" + numOfLocalfunctions;
			numOfLocalfunctions++;
		}
		Frames.frames.put(decl, new Frame(new Label(label),context.depth, context.locsSize, context.argsSize));
		return null;
	}

	@Override
	public Object visit(AbsCompDecl compDecl, Context visArg){
		RecContext context = (RecContext) visArg;
		SemType type = SemAn.isType.get(compDecl.type);
		Frames.accesses.put(compDecl, new RelAccess(type.size(), context.compsSize, 0));
		context.compsSize += type.size();
		return null;
	}

	@Override
	public Object visit(AbsParDecl parDecl, FrmEvaluator.Context visArg){
		SemType type = SemAn.isType.get(parDecl.type);
		FunContext context = ((FunContext) visArg);
		Frames.accesses.put(parDecl, new RelAccess(type.size(), context.parsSize, context.depth));
		context.parsSize += type.size();
		return null;
	}

	@Override
	public Object visit(AbsFunName funName, FrmEvaluator.Context visArg){
		FunContext context = (FunContext) visArg;
		int size = (Integer) funName.args.accept(this, visArg);
		if (size > context.argsSize){
			context.argsSize = size;
		}
		return null;
	}

	@Override
	public Object visit(AbsArgs args, Context visArgs){
		int size = 0;
		for (AbsExpr expr : args.args()){
			size += SemAn.ofType.get(expr).size();
		}
		return size;
	}
}
