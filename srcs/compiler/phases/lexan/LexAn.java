/**
 * @author sliva
 */
package compiler.phases.lexan;

import java.io.*;
import java.util.HashMap;

import compiler.common.report.*;
import compiler.data.symbol.*;
import compiler.phases.*;

/**
 * Lexical analysis.
 * 
 * @author sliva
 */
public class LexAn extends Phase {

	/** The name of the source file. */
	private final String srcFileName;

	/** The source file reader. */
	private final BufferedReader srcFile;

	/**
	 * Constructs a new phase of lexical analysis.
	 */
	public LexAn() {
		super("lexan");
		srcFileName = compiler.Main.cmdLineArgValue("--src-file-name");
		try {
			srcFile = new BufferedReader(new FileReader(srcFileName));
		} catch (IOException ___) {
			throw new Report.Error("Cannot open source file '" + srcFileName + "'.");
		}
		initialize();
	}

    private void initialize(){
        keywordMap = new HashMap<>();
        keywordMap.put("arr",Symbol.Term.ARR);
        keywordMap.put("bool",Symbol.Term.BOOL);
        keywordMap.put("char",Symbol.Term.CHAR);
        keywordMap.put("del",Symbol.Term.DEL);
        keywordMap.put("do",Symbol.Term.DO);
        keywordMap.put("else",Symbol.Term.ELSE);
        keywordMap.put("end",Symbol.Term.END);
        keywordMap.put("fun",Symbol.Term.FUN);
        keywordMap.put("if",Symbol.Term.IF);
        keywordMap.put("int",Symbol.Term.INT);
        keywordMap.put("new",Symbol.Term.NEW);
        keywordMap.put("ptr",Symbol.Term.PTR);
        keywordMap.put("rec",Symbol.Term.REC);
        keywordMap.put("then",Symbol.Term.THEN);
        keywordMap.put("typ",Symbol.Term.TYP);
        keywordMap.put("var",Symbol.Term.VAR);
        keywordMap.put("void",Symbol.Term.VOID);
        keywordMap.put("where",Symbol.Term.WHERE);
        keywordMap.put("while",Symbol.Term.WHILE);

        keywordMap.put("none",Symbol.Term.VOIDCONST);
        keywordMap.put("true",Symbol.Term.BOOLCONST);
        keywordMap.put("false",Symbol.Term.BOOLCONST);
        keywordMap.put("null",Symbol.Term.PTRCONST);

        readNextCharacter();
    }

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException ___) {
			Report.warning("Cannot close source file '" + this.srcFileName + "'.");
		}
		super.close();
	}

	/**
	 * The lexer.
	 * 
	 * This method returns the next symbol from the source file. To perform the
	 * lexical analysis of the entire source file, this method must be called until
	 * it returns EOF. This method calls {@link #lexify()}, logs its result if
	 * requested, and returns it.
	 * 
	 * @return The next symbol from the source file or EOF if no symbol is available
	 *         any more.
	 */
	public Symbol lexer() {
		Symbol symb = lexify();
		if (symb.token != Symbol.Term.EOF)
			symb.log(logger);
		return symb;
	}

	private HashMap<String, Symbol.Term> keywordMap;
    private int character = -1;
    private String lexeme;

    private int rowLocation = 1;
    private int columnLocation = 0;
    private int startRowLocation = 1;
    private int startColumnLocation = 1;
    private int endRowLocation = 1;
    private int endColumnLocation = 1;
	/**
	 * Performs the lexical analysis of the source file.
	 * 
	 * This method returns the next symbol from the source file. To perform the
	 * lexical analysis of the entire source file, this method must be called until
	 * it returns EOF.
	 * 
	 * @return The next symbol from the source file or EOF if no symbol is available
	 *         any more.
	 */
	private Symbol lexify() {
	    if (character == -1){
	        return new Symbol(Symbol.Term.EOF, "EOF", getLocation());
        }

        String ch = (char) character + "";
	    lexeme = "";
	    startRowLocation = rowLocation;
	    startColumnLocation = columnLocation;

	    if (ch.equals("#")){
	        return readComment();
        } else if (ch.equals("'")){
	        return readCharConstant();
        } else if (ch.equals("\"")){
	        return readStringConstant();
        } else if ('0'<=ch.charAt(0) && ch.charAt(0)<='9'){
	        return readIntConstant();
        } else if (('A'<=ch.charAt(0) && ch.charAt(0)<='Z') ||
                ('a'<=ch.charAt(0) && ch.charAt(0)<='z') || ch.charAt(0)=='_') {
	        return readReservedWord();
        } else if (ch.equals(" ") || ch.equals("\n") || ch.equals("\t") || ch.equals("\r")){
	        readNextCharacter();
	        if(character == -1){
	            return new Symbol(Symbol.Term.EOF,"EOF", getLocation());
            }
	        ch = (char) character + "";
	        while (ch.equals(" ") || ch.equals("\n") || ch.equals("\t") || ch.equals("\r")){
	            readNextCharacter();
                if(character == -1){
                    return new Symbol(Symbol.Term.EOF,"EOF", getLocation());
                }
                ch = (char) character + "";
            }
            return lexify();
        } else {
            return getOtherSymbol(ch);
        }
	}

    // Lexeme Construction Methods

    private Symbol readComment(){
	    String ch = (char) character + "";
	    while (!ch.equals("\n")){
	        readNextCharacter();
	        if(character == -1){
                return new Symbol(Symbol.Term.EOF, "EOF", getLocation());
            }
            ch = (char) character + "";
        }
        readNextCharacter();
	    return lexify();
    }

    private Symbol readCharConstant(){
        String ch = (char) character + "";
        lexeme += ch;
        readNextCharacter();
        if (character>=32 && character<=126){
            lexeme += (char) character;
            readNextCharacter();
        } else {
            throw throwError("Char not valid");
        }
        if(character == -1){
            throw throwError("Char not closed");
        }
        ch = (char) character + "";
        if (ch.equals("'")){
            lexeme += (char) character;
            readNextCharacter();
        } else {
            throw throwError("Char not valid, should be '.");
        }
	    return new Symbol(Symbol.Term.CHARCONST, lexeme, getLocation());
    }

    private Symbol readStringConstant(){
        String ch = (char) character + "";
        lexeme += ch;
        readNextCharacter();
        while (character>=32 && character<=126 && character!=34){
            lexeme += (char) character;
            readNextCharacter();
        }
        if(character==34){
            lexeme += (char) character;
            readNextCharacter();
            return new Symbol(Symbol.Term.STRCONST, lexeme, getLocation());
        } else if (character==10 || character==13 || character==-1){
            throw throwError("String not closed.");
        } else {
            startColumnLocation = columnLocation;
            endColumnLocation = columnLocation;
            throw throwError("Invalid character.");
        }
    }

    private Symbol readIntConstant(){
	    lexeme += (char) character;
	    readNextCharacter();
        if (character == -1){
            return new Symbol(Symbol.Term.INTCONST, lexeme, getLocation());
        }
	    char ch = (char) character;
	    while ('0'<=ch && ch<='9'){
	        lexeme += ch;
	        readNextCharacter();
            if (character == -1){
                return new Symbol(Symbol.Term.INTCONST, lexeme, getLocation());
            }
	        ch = (char) character;
        }
        return new Symbol(Symbol.Term.INTCONST, lexeme, getLocation());
    }

    private Symbol readReservedWord(){
        lexeme += (char) character;
        readNextCharacter();
        if (character == -1){
            return new Symbol(keywordMap.getOrDefault(lexeme, Symbol.Term.IDENTIFIER), lexeme, getLocation());
        }
        char ch = (char) character;
        while (('A'<=ch && ch<='Z') || ('a'<=ch && ch<='z') ||
                ('0'<=ch && ch<='9') || ch=='_'){
            lexeme += ch;
            readNextCharacter();
            if (character == -1){
                return new Symbol(keywordMap.getOrDefault(lexeme, Symbol.Term.IDENTIFIER), lexeme, getLocation());
            }
            ch = (char) character;
        }

        return new Symbol(keywordMap.getOrDefault(lexeme, Symbol.Term.IDENTIFIER), lexeme, getLocation());
    }

    private Symbol getOtherSymbol(String ch) {
        lexeme += ch;
        switch (ch){
            case "!":{
                readNextCharacter();
                if (character == -1){
                    return new Symbol(Symbol.Term.NOT, lexeme, getLocation());
                }
                ch = (char) character + "";
                if (ch.equals("=")){
                    lexeme += ch;
                    readNextCharacter();
                    return new Symbol(Symbol.Term.NEQ, lexeme, getLocation());
                } else {
                    return new Symbol(Symbol.Term.NOT, lexeme, getLocation());
                }
            }
            case "|":{
                readNextCharacter();
                return new Symbol(Symbol.Term.IOR, lexeme, getLocation());
            }
            case "^":{
                readNextCharacter();
                return new Symbol(Symbol.Term.XOR, lexeme, getLocation());
            }
            case "&":{
                readNextCharacter();
                return new Symbol(Symbol.Term.AND, lexeme, getLocation());
            }
            case "=":{
                readNextCharacter();
                if (character == -1){
                    return new Symbol(Symbol.Term.ASSIGN, lexeme, getLocation());
                }
                ch = (char) character + "";
                if (ch.equals("=")){
                    lexeme += ch;
                    readNextCharacter();
                    return new Symbol(Symbol.Term.EQU, lexeme, getLocation());
                } else {
                    return new Symbol(Symbol.Term.ASSIGN, lexeme, getLocation());
                }
            }
            case "<":{
                readNextCharacter();
                if (character == -1){
                    return new Symbol(Symbol.Term.LTH, lexeme, getLocation());
                }
                ch = (char) character + "";
                if (ch.equals("=")){
                    lexeme += ch;
                    readNextCharacter();
                    return new Symbol(Symbol.Term.LEQ, lexeme, getLocation());
                } else {
                    return new Symbol(Symbol.Term.LTH, lexeme, getLocation());
                }
            }
            case ">":{
                readNextCharacter();
                if (character == -1){
                    return new Symbol(Symbol.Term.GTH, lexeme, getLocation());
                }
                ch = (char) character + "";
                if (ch.equals("=")){
                    lexeme += ch;
                    readNextCharacter();
                    return new Symbol(Symbol.Term.GEQ, lexeme, getLocation());
                } else {
                    return new Symbol(Symbol.Term.GTH, lexeme, getLocation());
                }
            }
            case "+":{
                readNextCharacter();
                return new Symbol(Symbol.Term.ADD, lexeme, getLocation());
            }
            case "-":{
                readNextCharacter();
                return new Symbol(Symbol.Term.SUB, lexeme, getLocation());
            }
            case "*":{
                readNextCharacter();
                return new Symbol(Symbol.Term.MUL, lexeme, getLocation());
            }
            case "/":{
                readNextCharacter();
                return new Symbol(Symbol.Term.DIV, lexeme, getLocation());
            }
            case "%":{
                readNextCharacter();
                return new Symbol(Symbol.Term.MOD, lexeme, getLocation());
            }
            case "$":{
                readNextCharacter();
                return new Symbol(Symbol.Term.ADDR, lexeme, getLocation());
            }
            case "@":{
                readNextCharacter();
                return new Symbol(Symbol.Term.DATA, lexeme, getLocation());
            }
            case ".":{
                readNextCharacter();
                return new Symbol(Symbol.Term.DOT, lexeme, getLocation());
            }
            case ",":{
                readNextCharacter();
                return new Symbol(Symbol.Term.COMMA, lexeme, getLocation());
            }
            case ":":{
                readNextCharacter();
                return new Symbol(Symbol.Term.COLON, lexeme, getLocation());
            }
            case ";":{
                readNextCharacter();
                return new Symbol(Symbol.Term.SEMIC, lexeme, getLocation());
            }
            case "[":{
                readNextCharacter();
                return new Symbol(Symbol.Term.LBRACKET, lexeme, getLocation());
            }
            case "]":{
                readNextCharacter();
                return new Symbol(Symbol.Term.RBRACKET, lexeme, getLocation());
            }
            case "(":{
                readNextCharacter();
                return new Symbol(Symbol.Term.LPARENTHESIS, lexeme, getLocation());
            }
            case ")":{
                readNextCharacter();
                return new Symbol(Symbol.Term.RPARENTHESIS, lexeme, getLocation());
            }
            case "{":{
                readNextCharacter();
                return new Symbol(Symbol.Term.LBRACE, lexeme, getLocation());
            }
            case "}":{
                readNextCharacter();
                return new Symbol(Symbol.Term.RBRACE, lexeme, getLocation());
            }
            default:
                endColumnLocation++;
                throw throwError("Invalid character.");
        }
    }


    // Helper Methods

    private void readNextCharacter(){
	    try {
	        character = srcFile.read();
	        updateLocation();
        } catch (Exception e){
	        e.printStackTrace();
        }
    }

    private void updateLocation(){
	    endRowLocation = rowLocation;
	    endColumnLocation = columnLocation;
	    if (character == -1){
	        columnLocation ++;
	        return;
        }
        String chr = (char) character + "";
        switch (chr) {
            case "\n":
                rowLocation += 1;
                columnLocation = 0;
                break;
            case "\t":
                columnLocation += 8;
                break;
            default:
                columnLocation++;
        }
    }

    private Locatable getLocation(){
	    return new Location(startRowLocation, startColumnLocation, endRowLocation, endColumnLocation);
    }

    private Report.Error throwError(String message){
        return new Report.Error(getLocation(), message);
    }
}
