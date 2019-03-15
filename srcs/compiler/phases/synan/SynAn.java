/**
 * @author sliva
 */
package compiler.phases.synan;

import compiler.common.report.*;
import compiler.data.symbol.*;
import compiler.data.dertree.*;
import compiler.phases.*;
import compiler.phases.lexan.*;

/**
 * Syntax analysis.
 * 
 * @author sliva
 */
public class SynAn extends Phase {

	/** The derivation tree of the program being compiled. */
	public static DerTree derTree = null;

	/** The lexical analyzer used by this syntax analyzer. */
	private final LexAn lexAn;

	/**
	 * Constructs a new phase of syntax analysis.
	 */
	public SynAn() {
		super("synan");
		lexAn = new LexAn();
	}

	@Override
	public void close() {
		lexAn.close();
		super.close();
	}

	/**
	 * The parser.
	 * 
	 * This method constructs a derivation tree of the program in the source file.
	 * It calls method {@link #parseSource()} that starts a recursive descent parser
	 * implementation of an LL(1) parsing algorithm.
	 */
	public void parser() {
		currSymb = lexAn.lexer();
		derTree = parseSource();
		if (currSymb.token != Symbol.Term.EOF)
			throw new Report.Error(currSymb, "Unexpected '" + currSymb + "' at the end of a program.");
	}

	/** The lookahead buffer (of length 1). */
	private Symbol currSymb = null;

	/**
	 * Appends the current symbol in the lookahead buffer to a derivation tree node
	 * (typically the node of the derivation tree that is currently being expanded
	 * by the parser) and replaces the current symbol (just added) with the next
	 * input symbol.
	 * 
	 * @param node The node of the derivation tree currently being expanded by the
	 *             parser.
	 */
	private void add(DerNode node) {
		if (currSymb == null)
			throw new Report.InternalError();
		node.add(new DerLeaf(currSymb));
		currSymb = lexAn.lexer();
	}

	/**
	 * If the current symbol is the expected terminal, appends the current symbol in
	 * the lookahead buffer to a derivation tree node (typically the node of the
	 * derivation tree that is currently being expanded by the parser) and replaces
	 * the current symbol (just added) with the next input symbol. Otherwise,
	 * produces the error message.
	 * 
	 * @param node     The node of the derivation tree currently being expanded by
	 *                 the parser.
	 * @param token    The expected terminal.
	 * @param errorMsg The error message.
	 */
	private void add(DerNode node, Symbol.Term token, String errorMsg) {
		if (currSymb == null)
			throw new Report.InternalError();
		if (currSymb.token == token) {
			node.add(new DerLeaf(currSymb));
			currSymb = lexAn.lexer();
		} else
			throw new Report.Error(currSymb, errorMsg);
	}

	private DerNode parseSource() {
		DerNode node = new DerNode(DerNode.Nont.Source);
		node.add(parseDecls());
		return node;
	}

	private DerNode parseDecls(){
		DerNode node = new DerNode(DerNode.Nont.Decls);
		node.add(parseDecl());
		node.add(parseDeclsRest());
		return node;
	}

	private DerNode parseDecl(){
		DerNode node = new DerNode(DerNode.Nont.Decls);
		switch (currSymb.token){
			case TYP:
			case VAR: {
				add(node);
				add(node, Symbol.Term.IDENTIFIER, createErrorMessage(Symbol.Term.IDENTIFIER));
				add(node, Symbol.Term.COLON, createErrorMessage(Symbol.Term.COLON));
				node.add(parseType());
				add(node, Symbol.Term.SEMIC, createErrorMessage(Symbol.Term.SEMIC));
				break;
			}
			case FUN:{
				add(node);
				add(node, Symbol.Term.IDENTIFIER, createErrorMessage(Symbol.Term.IDENTIFIER));
				add(node, Symbol.Term.LPARENTHESIS, createErrorMessage(Symbol.Term.LPARENTHESIS));
				node.add(parseParDeclsEps());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				add(node, Symbol.Term.COLON, createErrorMessage(Symbol.Term.COLON));
				node.add(parseType());
				node.add(parseBodyEps());
				add(node, Symbol.Term.SEMIC, createErrorMessage(Symbol.Term.SEMIC));
				break;
			}
			default: {
				throw createError("Decl");
			}
		}
		return node;
	}

	private DerNode parseDeclsRest(){
		DerNode node = new DerNode(DerNode.Nont.DeclsRest);
		switch (currSymb.token){
			case TYP:
			case VAR:
			case FUN:{
				node.add(parseDecls());
				break;
			}
			case EOF:
			case RBRACE: {
				break;
			}
			default: {
                throw createError("DeclsRest");
			}
		}
		return node;
	}

	private DerNode parseParDeclsEps(){
		DerNode node = new DerNode(DerNode.Nont.ParDeclsEps);
		if (currSymb.token.equals(Symbol.Term.RPARENTHESIS)){
			return node;
		} else {
			node.add(parseParDecls());
			return node;
		}
	}

	private DerNode parseParDecls(){
		DerNode node = new DerNode(DerNode.Nont.ParDecls);
		node.add(parseParDecl());
		node.add(parseParDeclsRest());
		return node;
	}

	private DerNode parseParDecl(){
		DerNode node = new DerNode(DerNode.Nont.ParDecl);
		if (currSymb.token.equals(Symbol.Term.IDENTIFIER)) {
			add(node);
			add(node, Symbol.Term.COLON, createErrorMessage(Symbol.Term.COLON));
			node.add(parseType());
			return node;
		} else {
			throw createError("ParDecl");
		}
	}

	private DerNode parseParDeclsRest(){
		DerNode node = new DerNode(DerNode.Nont.ParDeclsRest);
		switch (currSymb.token){
			case RPARENTHESIS:{
				break;
			}
			case COMMA: {
				add(node);
				node.add(parseParDecls());
				break;
			}
			default: {
				throw createError("ParDeclsRest");
			}
		}
		return node;
	}

	private DerNode parseBodyEps(){
		DerNode node = new DerNode(DerNode.Nont.BodyEps);
		switch (currSymb.token){
			case SEMIC: {
				break;
			}
			case ASSIGN:
				add(node);
				node.add(parseDisjExpr());
				break;
			default: {
				throw createError("BodyEps");
			}
		}
		return node;
	}

	private DerNode parseType(){
		DerNode node = new DerNode(DerNode.Nont.Type);
		switch (currSymb.token){
			case IDENTIFIER:
			case VOID:
			case INT:
			case CHAR:
			case BOOL:{
				add(node);
				break;
			}
			case LPARENTHESIS:{
				add(node);
				node.add(parseType());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				break;
			}
			case ARR: {
				add(node);
				add(node, Symbol.Term.LBRACKET, createErrorMessage(Symbol.Term.LBRACKET));
				node.add(parseDisjExpr());
				add(node, Symbol.Term.RBRACKET, createErrorMessage(Symbol.Term.RBRACKET));
				node.add(parseType());
				break;
			}
			case PTR: {
				add(node);
				node.add(parseType());
				break;
			}
			case REC: {
				add(node);
				add(node, Symbol.Term.LPARENTHESIS, createErrorMessage(Symbol.Term.LPARENTHESIS));
				node.add(parseCompDecls());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				break;
			}
			default: {
				throw createError("Type");
			}
		}
		return node;
	}

	private DerNode parseCompDecls(){
		DerNode node = new DerNode(DerNode.Nont.CompDecls);
		node.add(parseCompDecl());
		node.add(parseCompDeclsRest());
		return node;
	}

	private DerNode parseCompDecl(){
		DerNode node = new DerNode(DerNode.Nont.CompDecl);
		if (currSymb.token.equals(Symbol.Term.IDENTIFIER)){
			add(node);
			add(node, Symbol.Term.COLON,createErrorMessage(Symbol.Term.COLON));
			node.add(parseType());
			return node;
		} else {
			throw createError("CompDecl");
		}
	}

	private DerNode parseCompDeclsRest(){
		DerNode node = new DerNode(DerNode.Nont.CompDeclsRest);
		switch (currSymb.token){
			case RPARENTHESIS:{
				break;
			}
			case COMMA:{
				add(node);
				node.add(parseCompDecls());
				break;
			}
			default: {
				throw createError("CompDeclsRest");
			}
		}
		return node;
	}

	private DerNode parseDisjExpr(){
		DerNode node = new DerNode(DerNode.Nont.DisjExpr);
		node.add(parseConjExpr());
		node.add(parseDisjExprRest());
		return node;
	}

	private DerNode parseDisjExprRest(){
		DerNode node = new DerNode(DerNode.Nont.DisjExprRest);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case COMMA:
			case ASSIGN:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case RBRACE: {
				break;
			}
			case IOR:
			case XOR: {
				add(node);
				node.add(parseDisjExpr());
				break;
			}
			default: {
				throw createError("DisjExprRest");
			}
		}
		return node;
	}

	private DerNode parseConjExpr(){
		DerNode node = new DerNode(DerNode.Nont.ConjExpr);
		node.add(parseRelExpr());
		node.add(parseConjExprRest());
		return node;
	}

	private DerNode parseConjExprRest(){
		DerNode node = new DerNode(DerNode.Nont.ConjExprRest);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case COMMA:
			case ASSIGN:
			case RBRACKET:
			case RBRACE:
			case WHERE:
			case THEN:
			case DO:
			case IOR:
			case XOR: {
				break;
			}
			case AND: {
				add(node);
				node.add(parseConjExpr());
				break;
			}
			default: {
				throw createError("ConjExprRest");
			}
		}
		return node;
	}

	private DerNode parseRelExpr(){
		DerNode node = new DerNode(DerNode.Nont.RelExpr);
		node.add(parseAddExpr());
		node.add(parseRelExprRest());
		return node;
	}

	private DerNode parseRelExprRest(){
		DerNode node = new DerNode(DerNode.Nont.RelExprRest);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case COMMA:
			case ASSIGN:
			case RBRACKET:
			case RBRACE:
			case WHERE:
			case THEN:
			case DO:
			case IOR:
			case XOR:
			case AND: {
				break;
			}
			case EQU:
			case NEQ:
			case LEQ:
			case GEQ:
			case LTH:
			case GTH: {
				add(node);
				node.add(parseAddExpr());
				break;
			}
			default: {
				throw createError("RelExprRest");
			}
		}
		return node;
	}

	private DerNode parseAddExpr(){
		DerNode node = new DerNode(DerNode.Nont.AddExpr);
		node.add(parseMulExpr());
		node.add(parseAddExprRest());
		return node;
	}

	private DerNode parseAddExprRest(){
		DerNode node = new DerNode(DerNode.Nont.AddExprRest);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case COMMA:
			case ASSIGN:
			case RBRACKET:
			case RBRACE:
			case WHERE:
			case THEN:
			case DO:
			case IOR:
			case XOR:
			case AND:
			case EQU:
			case NEQ:
			case LEQ:
			case GEQ:
			case LTH:
			case GTH: {
				break;
			}
			case ADD:
			case SUB:{
				add(node);
				node.add(parseAddExpr());
				break;
			}
			default: {
				throw createError("AddExprRest");
			}
		}
		return node;
	}

	private DerNode parseMulExpr(){
		DerNode node = new DerNode(DerNode.Nont.MulExpr);
		node.add(parsePrefExpr());
		node.add(parseMulExprRest());
		return node;
	}

	private DerNode parseMulExprRest(){
		DerNode node = new DerNode(DerNode.Nont.MulExprRest);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case COMMA:
			case ASSIGN:
			case RBRACKET:
			case RBRACE:
			case WHERE:
			case THEN:
			case DO:
			case IOR:
			case XOR:
			case AND:
			case EQU:
			case NEQ:
			case LEQ:
			case GEQ:
			case LTH:
			case GTH:
			case ADD:
			case SUB:{
				break;
			}
			case MUL:
			case DIV:
			case MOD:{
				add(node);
				node.add(parseMulExpr());
				break;
			}
			default: {
				throw createError("MulExprRest");
			}
		}
		return node;
	}

	private DerNode parsePrefExpr(){
		DerNode node = new DerNode(DerNode.Nont.PrefExpr);
		switch (currSymb.token){
			case IDENTIFIER:
			case LPARENTHESIS:
			case LBRACE:
			case CHARCONST:
			case BOOLCONST:
			case INTCONST:
			case PTRCONST:
			case STRCONST:
			case VOIDCONST: {
				node.add(parsePstfExpr());
				break;
			}
			case ADD:
			case SUB:
			case NOT:
			case DATA:
			case ADDR: {
				add(node);
				node.add(parsePrefExpr());
				break;
			}
			case NEW: {
				add(node);
				add(node, Symbol.Term.LPARENTHESIS, createErrorMessage(Symbol.Term.LPARENTHESIS));
				node.add(parseType());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				break;
			}
			case DEL: {
				add(node);
				add(node, Symbol.Term.LPARENTHESIS, createErrorMessage(Symbol.Term.LPARENTHESIS));
				node.add(parseDisjExpr());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				break;
			}
			default: {
				throw createError("PrefExpr");
			}
		}
		return node;
	}

	private DerNode parsePstfExpr(){
		DerNode node = new DerNode(DerNode.Nont.PstfExpr);
		node.add(parseExpr());
		node.add(parsePstfExpRest());
		return node;
	}

	private DerNode parsePstfExpRest(){
		DerNode node = new DerNode(DerNode.Nont.PstfExprRest);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case RBRACE:
			case COMMA:
			case ASSIGN:
			case RBRACKET:
			case WHERE:
			case THEN:
			case DO:
			case IOR:
			case XOR:
			case AND:
			case EQU:
			case NEQ:
			case LEQ:
			case GEQ:
			case LTH:
			case GTH:
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD: {
				break;
			}
			case DOT: {
				add(node);
				add(node, Symbol.Term.IDENTIFIER, createErrorMessage(Symbol.Term.IDENTIFIER));
				node.add(parsePstfExpRest());
				break;
			}
			case LBRACKET: {
				add(node);
				node.add(parseDisjExpr());
				add(node, Symbol.Term.RBRACKET, createErrorMessage(Symbol.Term.RBRACKET));
				node.add(parsePstfExpRest());
				break;
			}
			default: {
				throw createError("PstfExprRest");
			}
		}
		return node;
	}

	private DerNode parseExpr(){
		DerNode node = new DerNode(DerNode.Nont.Expr);
		switch (currSymb.token){
			case IDENTIFIER:
			case CHARCONST:
			case BOOLCONST:
			case INTCONST:
			case STRCONST:
			case PTRCONST:
			case VOIDCONST:	{
				node.add(parseAtomExpr());
				break;
			}
			case LBRACE:{
				add(node);
				node.add(parseStmts());
				add(node, Symbol.Term.COLON, createErrorMessage(Symbol.Term.COLON));
				node.add(parseDisjExpr());
				node.add(parseWhereEps());
				add(node, Symbol.Term.RBRACE, createErrorMessage(Symbol.Term.RBRACE));
				break;
			}
			case LPARENTHESIS: {
				add(node);
				node.add(parseDisjExpr());
				node.add(parseCastEps());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				break;
			}
			default: {
				throw createError("Expr");
			}
		}
		return node;
	}

	private DerNode parseAtomExpr(){
		DerNode node = new DerNode(DerNode.Nont.AtomExpr);
		if (currSymb.token.equals(Symbol.Term.IDENTIFIER)){
			add(node);
			node.add(parseCallEps());
		} else {
			add(node);
		}
		return node;
	}

	private DerNode parseCallEps(){
		DerNode node = new DerNode(DerNode.Nont.CallEps);
		switch (currSymb.token){
			case COLON:
			case SEMIC:
			case RPARENTHESIS:
			case COMMA:
			case ASSIGN:
			case LBRACKET:
			case RBRACKET:
			case IOR:
			case XOR:
			case AND:
			case EQU:
			case NEQ:
			case LEQ:
			case GEQ:
			case LTH:
			case GTH:
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case DOT:
			case RBRACE:
			case WHERE:
			case THEN:
			case DO: {
				break;
			}
			case LPARENTHESIS: {
				add(node);
				node.add(parseArgsEps());
				add(node, Symbol.Term.RPARENTHESIS, createErrorMessage(Symbol.Term.RPARENTHESIS));
				break;
			}
			default: {
				throw createError("CallEps");
			}

		}
		return node;
	}

	private DerNode parseArgsEps(){
		DerNode node = new DerNode(DerNode.Nont.ArgsEps);
		if (currSymb.token.equals(Symbol.Term.RPARENTHESIS)){
			return node;
		} else {
			node.add(parseArgs());
			node.add(parseArgsRest());
			return node;
		}
	}

	private DerNode parseArgs(){
		DerNode node = new DerNode(DerNode.Nont.Args);
		node.add(parseDisjExpr());
		return node;
	}

	private DerNode parseArgsRest(){
		DerNode node = new DerNode(DerNode.Nont.ArgsRest);
		switch (currSymb.token){
			case RPARENTHESIS: {
				break;
			}
			case COMMA: {
				add(node);
				node.add(parseArgs());
				node.add(parseArgsRest());
				break;
			}
			default: {
				throw createError("ArgsRest");
			}
		}
		return node;
	}

	private DerNode parseCastEps(){
		DerNode node = new DerNode(DerNode.Nont.CastEps);
		switch (currSymb.token){
			case RPARENTHESIS: {
				break;
			}
			case COLON: {
				add(node);
				node.add(parseType());
				break;
			}
			default: {
				throw createError("CastEps");
			}
		}
		return node;
	}

	private DerNode parseWhereEps(){
		DerNode node = new DerNode(DerNode.Nont.WhereEps);
		switch (currSymb.token){
			case WHERE: {
				add(node, Symbol.Term.WHERE, "");
				node.add(parseDecls());
				break;
			}
			case RBRACE: {
				break;
			}
			default: {
				throw createError("WhereEps");
			}
		}
		return node;
	}

	private DerNode parseStmts(){
		DerNode node = new DerNode(DerNode.Nont.Stmts);
		node.add(parseStmt());
		node.add(parseStmtsRest());
		return node;
	}

	private DerNode parseStmt(){
		DerNode node = new DerNode(DerNode.Nont.Stmt);
		switch (currSymb.token){
			case IDENTIFIER:
			case LPARENTHESIS:
			case LBRACE:
			case ADD:
			case SUB:
			case NOT:
			case DATA:
			case ADDR:
			case NEW:
			case DEL:
			case VOIDCONST:
			case BOOLCONST:
			case INTCONST:
			case PTRCONST:
			case STRCONST:
			case CHARCONST: {
				node.add(parseDisjExpr());
				node.add(parseAssignEps());
				add(node, Symbol.Term.SEMIC, createErrorMessage(Symbol.Term.SEMIC));
				break;
			}
			case IF:{
				add(node);
				node.add(parseDisjExpr());
				add(node,Symbol.Term.THEN, createErrorMessage(Symbol.Term.THEN));
				node.add(parseStmts());
				node.add(parseElseEps());
				add(node,Symbol.Term.END, createErrorMessage(Symbol.Term.END));
				add(node,Symbol.Term.SEMIC, createErrorMessage(Symbol.Term.SEMIC));
				break;
			}
			case WHILE: {
				add(node);
				node.add(parseDisjExpr());
				add(node, Symbol.Term.DO, createErrorMessage(Symbol.Term.DO));
				node.add(parseStmts());
				add(node, Symbol.Term.END, createErrorMessage(Symbol.Term.END));
				add(node, Symbol.Term.SEMIC, createErrorMessage(Symbol.Term.SEMIC));
				break;
			}
			default: {
				throw createError("Stmt");
			}
		}
		return node;
	}

	private DerNode parseStmtsRest(){
		DerNode node = new DerNode(DerNode.Nont.StmtsRest);
		switch (currSymb.token){
			case IDENTIFIER:
			case LPARENTHESIS:
			case LBRACE:
			case ADD:
			case SUB:
			case NOT:
			case DATA:
			case ADDR:
			case NEW:
			case DEL:
			case IF:
			case WHILE:
			case VOIDCONST:
			case BOOLCONST:
			case INTCONST:
			case PTRCONST:
			case STRCONST:
			case CHARCONST:{
				node.add(parseStmts());
				break;
			}
			case COLON:
			case END:
			case ELSE:{
				break;
			}
			default: {
				throw createError("StmtsRest");
			}
		}
		return node;
	}

	private DerNode parseAssignEps(){
		DerNode node = new DerNode(DerNode.Nont.AssignEps);
		switch (currSymb.token){
			case SEMIC:{
				break;
			}
			case ASSIGN:{
				add(node);
				node.add(parseDisjExpr());
				break;
			}
			default: {
				throw createError("AssignEps");
			}
		}
		return node;
	}

	private DerNode parseElseEps(){
		DerNode node = new DerNode(DerNode.Nont.ElseEps);
		switch (currSymb.token){
			case END: {
				break;
			}
			case ELSE:{
				add(node);
				node.add(parseStmts());
				break;
			}
			default: {
				throw createError("ElseEps");
			}
		}
		return node;
	}

	private Report.Error createError(String nonterminal){
	    return new Report.Error(currSymb,
				String.format("Symbol '%s' [%s] unexpected in nonterminal '%s'.", currSymb, currSymb.token, nonterminal));
    }

    private String createErrorMessage(Symbol.Term expected){
		return String.format("Unexpected token: received %s instead of %s.", currSymb.token, expected);
	}
}
