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
		switch (currSymb.token){
			case TYP:
			case VAR:
			case FUN:{
				node.add(parseDecls());
				break;
			}
			default: {
			}
		}
		return node;
	}

	private DerNode parseDecls(){
		DerNode node = new DerNode(DerNode.Nont.Decls);
		switch (currSymb.token){
			case TYP:
			case VAR:
			case FUN:{
				node.add(parseDecl());
				node.add(parseDeclRest());
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseDecl(){
		DerNode node = new DerNode(DerNode.Nont.Decls);
		switch (currSymb.token){
			case TYP: {
				add(node, Symbol.Term.TYP, "");
				add(node, Symbol.Term.IDENTIFIER, "");
				add(node, Symbol.Term.COLON, "");
				node.add(parseType());
				add(node, Symbol.Term.SEMIC, "");
				break;
			}
			case VAR: {
				add(node, Symbol.Term.VAR, "");
				add(node, Symbol.Term.IDENTIFIER, "");
				add(node, Symbol.Term.COLON, "");
				node.add(parseType());
				add(node, Symbol.Term.SEMIC, "");
				break;
			}
			case FUN:{
				add(node, Symbol.Term.FUN, "");
				add(node, Symbol.Term.IDENTIFIER, "");
				add(node, Symbol.Term.LPARENTHESIS, "");
				node.add(parseParDeclsEps());
				add(node, Symbol.Term.RPARENTHESIS, "");
				add(node, Symbol.Term.COLON, "");
				node.add(parseType());
				node.add(parseBodyEps());
				add(node, Symbol.Term.SEMIC, "");
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseDeclRest(){
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

			}
		}
		return node;
	}

	private DerNode parseType(){
		DerNode node = new DerNode(DerNode.Nont.Type);
		switch (currSymb.token){
			case IDENTIFIER:{
				add(node, Symbol.Term.IDENTIFIER, "");
				break;
			}
			case LPARENTHESIS:{
				add(node, Symbol.Term.LPARENTHESIS, "");
				node.add(parseType());
				add(node, Symbol.Term.RPARENTHESIS, "");
				break;
			}
			case VOID:{
				add(node, Symbol.Term.VOID, "");
				break;
			}
			case INT:{
				add(node, Symbol.Term.INT, "");
				break;
			}
			case CHAR:{
				add(node, Symbol.Term.CHAR, "");
				break;
			}
			case BOOL:{
				add(node, Symbol.Term.BOOL, "");
				break;
			}
			case ARR: {
				add(node, Symbol.Term.ARR, "");
				add(node, Symbol.Term.LBRACKET, "");
				node.add(parseExpr());
				add(node, Symbol.Term.RBRACKET, "");
				node.add(parseType());
				break;
			}
			case PTR: {
				add(node, Symbol.Term.PTR, "");
				node.add(parseType());
				break;
			}
			case REC: {
				add(node, Symbol.Term.REC, "");
				add(node, Symbol.Term.LPARENTHESIS, "");
				node.add(parseCompDecls());
				add(node, Symbol.Term.RPARENTHESIS, "");
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseParDeclsEps(){
		DerNode node = new DerNode(DerNode.Nont.ParDeclsEps);
		switch (currSymb.token){
			case IDENTIFIER: {
				node.add(parseParDecls());
				break;
			}
			case RPARENTHESIS:{
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseParDecls(){
		DerNode node = new DerNode(DerNode.Nont.ParDecls);
		switch (currSymb.token){
			case IDENTIFIER: {
				node.add(parseParDecl());
				node.add(parseParDeclsRest());
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseParDecl(){
		DerNode node = new DerNode(DerNode.Nont.ParDecl);
		switch (currSymb.token){
			case IDENTIFIER:{
				add(node, Symbol.Term.IDENTIFIER, "");
				add(node, Symbol.Term.COLON, "");
				node.add(parseType());
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseParDeclsRest(){
		DerNode node = new DerNode(DerNode.Nont.ParDeclsRest);
		switch (currSymb.token){
			case COMMA: {
				add(node, Symbol.Term.COMMA, "");
				node.add(parseParDecls());
				break;
			}
			case RPARENTHESIS:{
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseBodyEps(){
		DerNode node = new DerNode(DerNode.Nont.BodyEps);
		switch (currSymb.token){
			case EOF:
			case RBRACE:
			case FUN:
			case VAR:
			case TYP: {
				break;
			}
			case ASSIGN:
				add(node,Symbol.Term.ASSIGN,"");
				node.add(parseExpr());
				break;
			default: {

			}
		}
		return node;
	}

	private DerNode parseCompDecls(){
		DerNode node = new DerNode(DerNode.Nont.CompDecls);
		switch (currSymb.token){
			case IDENTIFIER:{
				node.add(parseCompDecl());
				node.add(parseCompDeclsRest());
				break;
			}
			default:{

			}
		}
		return node;
	}

	private DerNode parseCompDecl(){
		DerNode node = new DerNode(DerNode.Nont.CompDecl);
		switch (currSymb.token){
			case IDENTIFIER:{
				add(node, Symbol.Term.IDENTIFIER,"");
				add(node, Symbol.Term.COLON,"");
				node.add(parseType());
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseCompDeclsRest(){
		DerNode node = new DerNode(DerNode.Nont.CompDeclsRest);
		switch (currSymb.token){
			case RPARENTHESIS:{
				break;
			}
			case COMMA:{
				add(node, Symbol.Term.COMMA, "");
				node.add(parseCompDecls());
				break;
			}
			default: {

			}
		}
		return node;
	}

	private DerNode parseExpr(){
		DerNode node = new DerNode(DerNode.Nont.Expr);
		switch (currSymb.token){
			case LBRACE:{
				add(node, Symbol.Term.LBRACE, "");
				node.add(parseStmts());
				add(node, Symbol.Term.COLON, "");
				node.add(parseExpr());
				node.add(parseWhereEps());
				add(node, Symbol.Term.RBRACE, "");
				break;
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
			default: {

			}
		}
		return node;
	}

	private DerNode parseStmts(){
		DerNode node = new DerNode(DerNode.Nont.Stmts);
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
				node.add(parseStmt());
				node.add(parseStmtsRest());
				break;
			}
			default: {

			}
		}
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
				node.add(parseExpr());
				node.add(parseAssignEps());
				add(node, Symbol.Term.SEMIC, "");
				break;
			}
			case IF:{
				add(node, Symbol.Term.IF, "");
				node.add(parseExpr());
				add(node,Symbol.Term.THEN,"");
				node.add(parseStmts());
				node.add(parseElseEps());
				add(node,Symbol.Term.END,"");
				add(node,Symbol.Term.SEMIC, "");
				break;
			}
			case WHILE: {
				add(node, Symbol.Term.WHILE, "");
				node.add(parseExpr());
				add(node, Symbol.Term.DO, "");
				node.add(parseStmts());
				add(node, Symbol.Term.END, "");
				add(node, Symbol.Term.SEMIC, "");
				break;
			}
			default: {

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
				add(node, Symbol.Term.ASSIGN, "");
				node.add(parseExpr());
				break;
			}
			default: {

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
				add(node, Symbol.Term.ELSE, "");
				node.add(parseStmts());
				break;
			}
			default: {

			}
		}
		return node;
	}
}
