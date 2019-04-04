/**
 * @author sliva 
 */
package compiler.phases.seman;

import java.util.*;
import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.data.type.property.*;

/**
 * Type resolving: the result is stored in {@link SemAn#declaresType},
 * {@link SemAn#isType}, and {@link SemAn#ofType}.
 * 
 * @author sliva
 */
public class TypeResolver extends AbsFullVisitor<SemType, TypeResolver.Phase> {

	public enum Phase {
		AddNamedTypes, MapTypes
	}

	/** Symbol tables of individual record types. */
	private final HashMap<SemRecType, SymbTable> symbTables = new HashMap<SemRecType, SymbTable>();

	@Override
	public SemType visit(AbsSource source, TypeResolver.Phase visArg){
		super.visit(source, Phase.AddNamedTypes);
		super.visit(source, Phase.MapTypes);
		return null;
	}

	@Override
    public SemType visit(AbsTypDecl decl, TypeResolver.Phase visArg){
		if (visArg == Phase.AddNamedTypes){
			SemAn.declaresType.put(decl, new SemNamedType(decl.name));
		} else if (visArg == Phase.MapTypes){
			SemType type = decl.type.accept(this, visArg);
			SemNamedType named = SemAn.declaresType.get(decl);
			named.define(type);
		}
		return null;
	}

	@Override
	public SemType visit(AbsAtomType atom, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			switch (atom.type){
				case INT: {
					return new SemIntType();
				}
				case BOOL: {
					return new SemBoolType();
				}
				case CHAR: {
					return new SemCharType();
				}
				case VOID: {
					return new SemVoidType();
				}
				default: {
					// Create error
				}
			}
		}
		return null;
	}

	@Override
	public SemType visit(AbsArrType arr, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			long length = -1;
			if (arr.len instanceof AbsAtomExpr && ((AbsAtomExpr) arr.len).type == AbsAtomExpr.Type.INT){
				try {
					length = Long.parseLong(((AbsAtomExpr) arr.len).expr);
				} catch (Exception e){
					// Create error
				}
			} else {
				// Create error
			}
			SemType type = arr.elemType.accept(this, visArg);
			if (length < 1){
				// Create error
			}
			if (type instanceof SemVoidType){
				// Create error
			}
			return new SemArrType(length, type);
		}
		return null;
	}

	@Override
	public SemType visit(AbsRecType rec, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			return rec.compDecls.accept(this, visArg);
		}
		return null;
	}

	@Override
	public SemType visit(AbsCompDecls compDecls, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			Vector<SemType> compTypes = new Vector<SemType>();
			for (AbsCompDecl comp : compDecls.compDecls()){
				SemType type = comp.accept(this, visArg);
				if (type instanceof SemVoidType){
					// Create error
				}
				compTypes.add(type);
			}
			SemRecType record = new SemRecType(compTypes);
			SymbTable recSymbols = new SymbTable();
			for (AbsCompDecl comp : compDecls.compDecls()){
				try {
					recSymbols.ins(comp.name, comp);
				} catch (Exception e){
					// Create error
				}
			}
			symbTables.put(record, recSymbols);
			return record;
		}
		return null;
	}

	@Override
	public SemType visit(AbsCompDecl compDecl, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			return compDecl.type.accept(this, visArg);
		}
		return null;
	}

	@Override
	public SemType visit(AbsPtrType ptr, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			SemType typ = ptr.ptdType.accept(this, visArg);
			return new SemPtrType(typ);
		}
		return null;
	}

	@Override
	public SemType visit(AbsTypName name, TypeResolver.Phase visArg){
		if (visArg == Phase.MapTypes){
			AbsDecl decl = SemAn.declaredAt.get(name);
			if (decl instanceof AbsTypDecl){
				return SemAn.declaresType.get((AbsTypDecl) decl);
			} else {
				return null;
			}
		}
		return null;
	}

}
