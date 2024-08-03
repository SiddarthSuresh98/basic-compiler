package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jlox.TokenType.*;


/**
 * Returns all the tokens in the source code, traversing the source code character by character
 * Even if error is found, scanning continues so that all errors can be reported to the user at once.
 */
public class Scanner {
	
	private final String sourceCode;
	private final List<Token> tokens = new ArrayList<>();
	private static final Map<String, TokenType> keywords;
	private int start = 0;
	private int current = 0;
	private int line = 1;
	
	static {
		keywords = new HashMap<>();
		keywords.put("and", TokenType.AND);
		keywords.put("class", TokenType.CLASS);
		keywords.put("else", TokenType.ELSE);
		keywords.put("false", TokenType.FALSE);
		keywords.put("for", TokenType.FOR);
		keywords.put("fun", TokenType.FUN);
		keywords.put("if", TokenType.IF);
		keywords.put("nil", TokenType.NIL);
		keywords.put("or", TokenType.OR);
		keywords.put("print", TokenType.PRINT);
		keywords.put("return", TokenType.RETURN);
		keywords.put("super", TokenType.SUPER);
		keywords.put("this", TokenType.THIS);
		keywords.put("true", TokenType.TRUE);
		keywords.put("var", TokenType.VAR);
		keywords.put("while", TokenType.WHILE);
	}
	
	//Constructor
	Scanner(String sourceCode){
		this.sourceCode = sourceCode;
	}
	
	//Scans tokens character by character and returns list of tokens
	List<Token> scanTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}
			tokens.add(new Token(TokenType.EOF, "", null, line));
			return tokens;
	}
	
	//Checks if end of source code is reached
	public boolean isAtEnd() {
		return current >= this.sourceCode.length();
	}
	
	//Scans tokens from source code
	private void scanToken() {
		char c = advance();
		switch (c) {
			
			//Single Character Lexemes
			case '(': addToken(TokenType.LEFT_PAREN); break;
			case ')': addToken(TokenType.RIGHT_PAREN); break;
			case '{': addToken(TokenType.LEFT_BRACE); break;
			case '}': addToken(TokenType.RIGHT_BRACE); break;
			case ',': addToken(TokenType.COMMA); break;
			case '.': addToken(TokenType.DOT); break;
			case '-': addToken(TokenType.MINUS); break;
			case '+': addToken(TokenType.PLUS); break;
			case ';': addToken(TokenType.SEMICOLON); break;
			case '*': addToken(TokenType.STAR); break;
			
			//Operators, maximal munch rule
			case '!':
				addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
				break;
			case '=':
				addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
				break;
			case '<':
				addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
				break;
			case '>':
				addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
				break;
			case '/':
				//check if it is a comment, not added as a token
				if(match('/')){
					while(peek() != '\n' && !isAtEnd()) advance();
				} else if(match('*')) {
					blockComment();
				} else {
					addToken(TokenType.SLASH);
				}
				break;
				
			//Ignore whitespaces
			case ' ':
			case '\r':
			case  '\t':
				break;
			
			case '\n':
				line++;
				break;
				
			//String literals
			case '"':
				string();
				break;
			
			default:
				if(isDigit(c)) {
					number();
				} else if(isAlpha(c)) {
					identifier();
				} else {
					Lox.error(line, "Unexpected character " + Character.toString(c) + " found");
					break;
				}
		}
	}
	
	//consumes a character and returns it
	private char advance() {
		current++;
		return sourceCode.charAt(current - 1);
	}
	
	//adds a token to list
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	
	//adds a token to list
	private void addToken(TokenType type, Object literal) {
		String text = sourceCode.substring(start, current);
		tokens.add(new Token(type,text,literal,line));
	}
	
	//checks if next character is expected character for multiple character tokens, consumes the character
	private boolean match(char expected) {
		if (isAtEnd()) return false;
		if (sourceCode.charAt(current) != expected) return false;
		current++;
		return true;
	}
	
	//returns next character but does not consume it
	private char peek() {
		if(isAtEnd()) {
			return '\0';
		}
		return sourceCode.charAt(current);
	}
	
	//returns next string literal and consumes all string literal characters
	private void string() {
		while(peek() != '"' && !isAtEnd()) {
			if(peek() == '\n') line++;
			advance();
		}
		
		if(isAtEnd()) {
			Lox.error(line, "Unterminated String literal.");
			return;
		}
		
		//for the closing '"'
		advance();
		
		//get string literal without quotes
		String value = sourceCode.substring(start+1, current-1);
		addToken(TokenType.STRING, value);
		return;
	}
	
	//checks if character is a digit
	private boolean isDigit(char c) {
		return c>='0' && c<='9';
	}
	
	//creates a token number (1234 or 1234.1234, .1234 and 1234. not allowed)
	private void number() {
		while(isDigit(peek())) advance();
		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();
			while (isDigit(peek())) advance();
		}
		addToken(TokenType.NUMBER, Double.parseDouble(sourceCode.substring(start, current)));
	}
	
	//Peeks two characters ahead
	private char peekNext() {
		if (current + 1 >= sourceCode.length()) return '\0';
		return sourceCode.charAt(current + 1);
	}
	
	//checks if character is an alphabet or underscore
	private boolean isAlpha(char c) {
		return (c>='a' && c<='z') || (c>='A' && c<='Z') || (c=='_');
	}
	
	//adds identifier to list of tokens, checks if identifier is a keyword
	private void identifier() {
		while(isAlphaNumeric(peek())) advance();
		String text = sourceCode.substring(start, current);
		TokenType type = keywords.get(text);
		if (type == null) type = TokenType.IDENTIFIER;
		addToken(type);
	}
	
	//checks if character is alpha numeric
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
	
	private void blockComment() {
		while(peek() != '*' && peekNext() != '/' && !isAtEnd()) {
			if(peek() == '\n') line++;
			advance();
		}
		if(isAtEnd()) {
			Lox.error(line, "Unterminated Block Comment.");
			return;
		}
		advance();
		advance();
		String value = sourceCode.substring(start+2, current-2);
		System.out.println("Block Comment: " + value);
		return;
		
	}
	
}
