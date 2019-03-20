/**
 * @author sliva
 */
package compiler.phases.abstr;

import java.util.*;
import compiler.common.report.*;
import compiler.data.abstree.attribute.AbsAttribute;
import compiler.data.abstree.visitor.AbsVisitor;
import compiler.data.dertree.*;
import compiler.data.dertree.visitor.*;
import compiler.data.abstree.*;
import compiler.data.symbol.Symbol;

/**
 * Transforms a derivation tree to an abstract syntax tree.
 * 
 * @author sliva
 */
public class AbsTreeConstructor implements DerVisitor<AbsTree, AbsTree> {

	@Override
	public AbsTree visit(DerLeaf leaf, AbsTree visArg) {
		throw new Report.InternalError();
	}

	@Override
	public AbsTree visit(DerNode node, AbsTree visArg) {
		try {
			switch (node.label) {

				case Source: {
					AbsDecls decls = (AbsDecls) node.subtree(0).accept(this, null);
					return new AbsSource(decls, decls);
				}

				case Decls: {
					Vector<AbsDecl> allDecls = new Vector<AbsDecl>();
					AbsDecl decl = (AbsDecl) node.subtree(0).accept(this, null);
					allDecls.add(decl);
					AbsDecls decls = (AbsDecls) node.subtree(1).accept(this, null);
					if (decls != null)
						allDecls.addAll(decls.decls());
					return new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls);
				}

				case Decl: {
					return parseDecl(node);
				}

				case DeclsRest: {
					if (node.numSubtrees() == 0) {
						return null;
					} else {
						return node.subtree(0).accept(this, null);
					}
				}

				case ParDeclsEps: {
					if (node.numSubtrees() == 0){
						return new AbsParDecls(new Location(0, 0), new Vector<AbsParDecl>());
					}
					return node.subtree(0).accept(this, null);
				}

				case ParDecls: {
					Vector<AbsParDecl> allParDec = new Vector<AbsParDecl>();
					AbsParDecl parDecl = (AbsParDecl) node.subtree(0).accept(this, null);
					allParDec.add(parDecl);
					AbsParDecls parDecls = (AbsParDecls) node.subtree(1).accept(this, null);
					if (parDecls != null)
						allParDec.addAll(parDecls.parDecls());
					return new AbsParDecls(new Location(parDecl, parDecls == null ? parDecl : parDecls), allParDec);
				}

				case ParDecl: {
					DerLeaf idLeaf = (DerLeaf) node.subtree(0);
					AbsType type = (AbsType) node.subtree(2).accept(this, null);
					return new AbsParDecl(node, idLeaf.symb.lexeme, type);
				}

				case ParDeclsRest: {
					if (node.numSubtrees() == 0) {
						return null;
					} else {
						return node.subtree(1).accept(this, null);
					}
				}

				case BodyEps: {
					if (node.numSubtrees() == 0){
						return null;
					} else {
						return node.subtree(1).accept(this, null);
					}
				}

				case Type: {
					return parseType(node);
				}

				case CompDecls: {
					Vector<AbsCompDecl> allCompDecls = new Vector<AbsCompDecl>();
					AbsCompDecl compDecl = (AbsCompDecl) node.subtree(0).accept(this, null);
					allCompDecls.add(compDecl);
					AbsCompDecls compDecls = (AbsCompDecls) node.subtree(1).accept(this, null);
					if (compDecls != null)
						allCompDecls.addAll(compDecls.compDecls());
					return new AbsCompDecls(new Location(compDecl, compDecls == null ? compDecl : compDecls), allCompDecls);
				}

				case CompDecl: {
					DerLeaf idLeaf = (DerLeaf) node.subtree(0);
					AbsType type = (AbsType) node.subtree(2).accept(this, null);
					return new AbsCompDecl(node, idLeaf.symb.lexeme, type);
				}

				case CompDeclsRest: {
					if (node.numSubtrees() == 0){
						return null;
					} else {
						return node.subtree(1).accept(this, null);
					}
				}

				case DisjExpr: {
					AbsTree conjExpr = node.subtree(0).accept(this, null);
					return node.subtree(1).accept(this, conjExpr);
				}

				case DisjExprRest: {
					if (node.numSubtrees()==0){
						return visArg;
					} else {
						DerLeaf op = (DerLeaf) node.subtree(0);
						AbsExpr conjExpr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsBinExpr.Oper operator;
						if (op.symb.token == Symbol.Term.XOR){
							operator = AbsBinExpr.Oper.XOR;
						} else if (op.symb.token == Symbol.Term.IOR){
							operator = AbsBinExpr.Oper.IOR;
						} else {
							operator = null;
						}
						return node.subtree(2).accept(this, new AbsBinExpr(new Location(visArg,node), operator, (AbsExpr) visArg, conjExpr));
					}
				}

				case ConjExpr: {
					AbsTree relExpr = node.subtree(0).accept(this, null);
					return node.subtree(1).accept(this, relExpr);
				}

				case ConjExprRest: {
					if (node.numSubtrees()==0){
						return visArg;
					} else {
						DerLeaf op = (DerLeaf) node.subtree(0);
						AbsExpr relExpr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsBinExpr.Oper operator;
						if (op.symb.token == Symbol.Term.AND){
							operator = AbsBinExpr.Oper.AND;
						} else {
							operator = null;
						}
						return node.subtree(2).accept(this, new AbsBinExpr(new Location(visArg,node), operator, (AbsExpr) visArg, relExpr));
					}
				}

				case RelExpr: {
					AbsTree addExpr = node.subtree(0).accept(this, null);
					return node.subtree(1).accept(this, addExpr);
				}

				case RelExprRest: {
					if (node.numSubtrees()==0){
						return visArg;
					} else {
						DerLeaf op = (DerLeaf) node.subtree(0);
						AbsExpr addExpr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsBinExpr.Oper operator;
						switch (op.symb.token){
							case EQU: {
								operator = AbsBinExpr.Oper.EQU;
								break;
							}
							case NEQ: {
								operator = AbsBinExpr.Oper.NEQ;
								break;
							}
							case LTH: {
								operator = AbsBinExpr.Oper.LTH;
								break;
							}
							case GTH: {
								operator = AbsBinExpr.Oper.GTH;
								break;
							}
							case LEQ: {
								operator = AbsBinExpr.Oper.LEQ;
								break;
							}
							case GEQ: {
								operator = AbsBinExpr.Oper.GEQ;
								break;
							}
							default: {
								operator = null;
							}
						}
						return new AbsBinExpr(new Location(visArg,node), operator, (AbsExpr) visArg, addExpr);
					}
				}

				case AddExpr: {
					AbsTree mulExpr = node.subtree(0).accept(this, null);
					return node.subtree(1).accept(this, mulExpr);
				}

				case AddExprRest: {
					if (node.numSubtrees()==0){
						return visArg;
					} else {
						DerLeaf op = (DerLeaf) node.subtree(0);
						AbsExpr mulExpr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsBinExpr.Oper operator;
						if (op.symb.token == Symbol.Term.ADD){
							operator = AbsBinExpr.Oper.ADD;
						} else if (op.symb.token == Symbol.Term.SUB){
							operator = AbsBinExpr.Oper.SUB;
						} else {
							operator = null;
						}
						return node.subtree(2).accept(this, new AbsBinExpr(new Location(visArg,node), operator, (AbsExpr) visArg, mulExpr));
					}
				}

				case MulExpr: {
					AbsTree prefExpr = node.subtree(0).accept(this, null);
					return node.subtree(1).accept(this, prefExpr);
				}

				case MulExprRest: {
					if (node.numSubtrees()==0){
						return visArg;
					} else {
						DerLeaf op = (DerLeaf) node.subtree(0);
						AbsExpr prefExpr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsBinExpr.Oper operator;
						if (op.symb.token == Symbol.Term.MUL){
							operator = AbsBinExpr.Oper.MUL;
						} else if (op.symb.token == Symbol.Term.DIV){
							operator = AbsBinExpr.Oper.DIV;
						} else if (op.symb.token == Symbol.Term.MOD){
							operator = AbsBinExpr.Oper.MOD;
						}else {
							operator = null;
						}
						return node.subtree(2).accept(this, new AbsBinExpr(new Location(visArg,node), operator, (AbsExpr) visArg, prefExpr));
					}
				}

				case PrefExpr: {
					if (node.numSubtrees()==1){
						return node.subtree(0).accept(this, null);
					} else {
						DerLeaf operator = (DerLeaf) node.subtree(0);
						switch (operator.symb.token){
							case NOT:{
								AbsExpr pstfExpr = (AbsExpr) node.subtree(1).accept(this, null);
								return new AbsUnExpr(node, AbsUnExpr.Oper.NOT, pstfExpr);
							}
							case ADD:{
								AbsExpr pstfExpr = (AbsExpr) node.subtree(1).accept(this, null);
								return new AbsUnExpr(node, AbsUnExpr.Oper.ADD, pstfExpr);
							}
							case SUB:{
								AbsExpr pstfExpr = (AbsExpr) node.subtree(1).accept(this, null);
								return new AbsUnExpr(node, AbsUnExpr.Oper.SUB, pstfExpr);
							}
							case DATA:{
								AbsExpr pstfExpr = (AbsExpr) node.subtree(1).accept(this, null);
								return new AbsUnExpr(node, AbsUnExpr.Oper.DATA, pstfExpr);
							}
							case ADDR:{
								AbsExpr pstfExpr = (AbsExpr) node.subtree(1).accept(this, null);
								return new AbsUnExpr(node, AbsUnExpr.Oper.ADDR, pstfExpr);
							}
							case NEW:{
								AbsType type = (AbsType) node.subtree(2).accept(this, null);
								return new AbsNewExpr(node, type);
							}
							case DEL:{
								AbsExpr delExpr = (AbsExpr) node.subtree(2).accept(this, null);
								return new AbsDelExpr(node, delExpr);
							}
						}
					}
				}

				case PstfExpr: {
					AbsExpr expr = (AbsExpr) node.subtree(0).accept(this, null);
					return node.subtree(1).accept(this, expr);
				}

				case PstfExprRest: {
					if (node.numSubtrees()==0){
						return visArg;
					} else {
						DerLeaf op = (DerLeaf) node.subtree(0);
						if (op.symb.token == Symbol.Term.LBRACKET){
							AbsExpr index = (AbsExpr) node.subtree(1).accept(this, null);
							return node.subtree(3).accept(this, new AbsArrExpr(node, (AbsExpr) visArg, index));
						} else if (op.symb.token == Symbol.Term.DOT) {
							DerLeaf idLeaf = (DerLeaf) node.subtree(1);
							//return node.subtree(2).accept(this, new )
							//TODO
							return visArg;
						} else {
							return null;
						}
					}
				}

				case Expr: {
					if(node.numSubtrees()==1){
						return node.subtree(0).accept(this, null);
					} else if (node.numSubtrees()==4) {
						AbsExpr expr = (AbsExpr) node.subtree(1).accept(this, null);
						return node.subtree(2).accept(this, expr);
					} else if (node.numSubtrees()==6) {
						AbsStmts stmts = (AbsStmts) node.subtree(1).accept(this, null);
						AbsExpr expr = (AbsExpr) node.subtree(3).accept(this, null);
						AbsDecls decls = (AbsDecls) node.subtree(4).accept(this, null);
						return new AbsBlockExpr(node, decls, stmts, expr);
					}
				}

				case AtomExpr: {
					DerLeaf leaf = (DerLeaf) node.subtree(0);
					switch (leaf.symb.token){
						case CHARCONST: {
							return new AbsAtomExpr(node, AbsAtomExpr.Type.CHAR, leaf.symb.lexeme);
						}
						case BOOLCONST: {
							return new AbsAtomExpr(node, AbsAtomExpr.Type.BOOL, leaf.symb.lexeme);
						}
						case STRCONST: {
							return new AbsAtomExpr(node, AbsAtomExpr.Type.STR, leaf.symb.lexeme);
						}
						case INTCONST: {
							return new AbsAtomExpr(node, AbsAtomExpr.Type.INT, leaf.symb.lexeme);
						}
						case PTRCONST: {
							return new AbsAtomExpr(node, AbsAtomExpr.Type.PTR, leaf.symb.lexeme);
						}
						case VOIDCONST: {
							return new AbsAtomExpr(node, AbsAtomExpr.Type.VOID, leaf.symb.lexeme);
						}
						case IDENTIFIER: {
							DerLeaf id = (DerLeaf) node.subtree(0);
							AbsTree args = node.subtree(1).accept(this, null);
							if (args == null){
								return new AbsVarName(node, id.symb.lexeme);
							} else {
								return new AbsFunName(node, id.symb.lexeme, (AbsArgs) args);
							}
						}
					}
				}

				case CallEps: {
					if (node.numSubtrees() == 0) {
						return null;
					} else {
						return node.subtree(1).accept(this, null);
					}
				}

				case ArgsEps: {
					if (node.numSubtrees() == 0){
						return new AbsArgs(new Location(0, 0), new Vector<AbsExpr>());
					} else {
						Vector<AbsExpr> allArgs = new Vector<AbsExpr>();
						AbsExpr arg = (AbsExpr) node.subtree(0).accept(this, null);
						allArgs.add(arg);
						AbsArgs args = (AbsArgs) node.subtree(1).accept(this, null);
						if (args != null)
							allArgs.addAll(args.args());
						return new AbsArgs(new Location(arg, args == null ? arg : args), allArgs);
					}
				}

				case Args: {
					return node.subtree(0).accept(this, null);
				}

				case ArgsRest: {
					if (node.numSubtrees() == 0){
						return null;
					} else {
						Vector<AbsExpr> allArgs = new Vector<AbsExpr>();
						AbsExpr arg = (AbsExpr) node.subtree(1).accept(this, null);
						allArgs.add(arg);
						AbsArgs args = (AbsArgs) node.subtree(2).accept(this, null);
						if (args != null)
							allArgs.addAll(args.args());
						return new AbsArgs(new Location(arg, args == null ? arg : args), allArgs);
					}
				}

				case CastEps: {
					if (node.numSubtrees() == 0){
						return visArg;
					} else {
						AbsType type = (AbsType) node.subtree(1).accept(this, null);
						return new AbsCastExpr(new Location(visArg, node),(AbsExpr) visArg, type);
					}
				}

				case WhereEps: {
					if (node.numSubtrees() == 0){
						return new AbsDecls(new Location(0, 0), new Vector<AbsDecl>());
					} else {
						return node.subtree(1).accept(this, null);
					}
				}

				case Stmts: {
					Vector<AbsStmt> allStmts = new Vector<AbsStmt>();
					AbsStmt stmt = (AbsStmt) node.subtree(0).accept(this, null);
					allStmts.add(stmt);
					AbsStmts stmts = (AbsStmts) node.subtree(1).accept(this, null);
					if (stmts != null)
						allStmts.addAll(stmts.stmts());
					return new AbsStmts(new Location(stmt, stmts == null ? stmt : stmts), allStmts);
				}

				case Stmt: {
					if (node.numSubtrees()==3){
						AbsExpr expr = (AbsExpr) node.subtree(0).accept(this, null);
						AbsExpr assign = (AbsExpr) node.subtree(1).accept(this, null);
						if (assign == null){
							return new AbsExprStmt(node,expr);
						} else {
							return new AbsAssignStmt(node, expr, assign);
						}
					} else if (node.numSubtrees()==6){
						AbsExpr expr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsStmts stmts = (AbsStmts) node.subtree(3).accept(this, null);
						return new AbsWhileStmt(node, expr, stmts);
					} else if (node.numSubtrees()==7){
						AbsExpr expr = (AbsExpr) node.subtree(1).accept(this, null);
						AbsStmts then = (AbsStmts) node.subtree(3).accept(this, null);
						AbsStmts el = (AbsStmts) node.subtree(4).accept(this, null);
						return new AbsIfStmt(node, expr, then, el);
					} else {
						return null;
					}
				}

				case StmtsRest: {
					if (node.numSubtrees()==0){
						return null;
					}
					else {
						return node.subtree(0).accept(this, null);
					}
				}

				case AssignEps: {
					if (node.numSubtrees()==0){
						return null;
					} else {
						return node.subtree(1).accept(this, null);
					}
				}

				case ElseEps: {
					if (node.numSubtrees()==0){
						return new AbsStmts(new Location(0, 0), new Vector<AbsStmt>());
					} else {
						return node.subtree(1).accept(this, null);
					}
				}
				default: {

				}
			}
		} catch (Exception e){
			throw new Report.Error(node, String.format("Error occured in a node with label: %s", node.label));
		}
		return null;
	}

	private AbsTree parseDecl(DerNode node) {
		DerLeaf decl = (DerLeaf) node.subtree(0);
		switch (decl.symb.token){
			case VAR: {
				DerLeaf leaf = (DerLeaf) node.subtree(1);
				String id = leaf.symb.lexeme;
				AbsType type = (AbsType) node.subtree(3).accept(this, null);
				return new AbsVarDecl(node, id, type);
			}
			case TYP: {
				DerLeaf leaf = (DerLeaf) node.subtree(1);
				String id = leaf.symb.lexeme;
				AbsType type = (AbsType) node.subtree(3).accept(this, null);
				return new AbsTypDecl(node, id, type);
			}
			case FUN: {
				DerLeaf leaf = (DerLeaf) node.subtree(1);
				String id = leaf.symb.lexeme;
				AbsParDecls params = (AbsParDecls) node.subtree(3).accept(this, null);
				AbsType type = (AbsType) node.subtree(6).accept(this, null);
				AbsExpr body = (AbsExpr) node.subtree(7).accept(this, null);
				if (body == null){
					return new AbsFunDecl(node, id, params, type);
				} else {
					return new AbsFunDef(node, id, params, type, body);
				}
			}
			default: {

			}
		}
		return null;
	}

	private AbsTree parseType(DerNode node) {
		DerLeaf typeDef = (DerLeaf) node.subtree(0);
		switch (typeDef.symb.token){
			case INT: {
				return new AbsAtomType(node, AbsAtomType.Type.INT);
			}
			case BOOL: {
				return new AbsAtomType(node, AbsAtomType.Type.BOOL);
			}
			case VOID: {
				return new AbsAtomType(node, AbsAtomType.Type.VOID);
			}
			case CHAR: {
				return new AbsAtomType(node, AbsAtomType.Type.CHAR);
			}
			case ARR: {
				AbsExpr lenght = (AbsExpr) node.subtree(2).accept(this, null);
				AbsType type = (AbsType) node.subtree(4).accept(this, null);
				return new AbsArrType(node, lenght, type);
			}
			case PTR: {
				AbsType type = (AbsType) node.subtree(1).accept(this, null);
				return new AbsPtrType(node, type);
			}
			case IDENTIFIER: {
				return new AbsTypName(node, typeDef.symb.lexeme);
			}
			case LPARENTHESIS: {
				return node.subtree(1).accept(this, null);
			}
			case REC: {
				AbsCompDecls compDecls = (AbsCompDecls) node.subtree(2).accept(this, null);
				return new AbsRecType(node, compDecls);
			}
			default: {

			}
		}
		return null;
	}

}
