package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemaLexer extends Lexer {
    public static class Token extends IElementType {
        public Token(String debugName) {
            super(debugName, SchemaLanguage.SCHEMA_LANGUAGE);
        }
    }

    public static SchemaLexer SCHEMA_LEXER = new SchemaLexer();
    public static IElementType COMMENT = new Token("COMMENT");
    public static IElementType IDENTIFIER = new Token("IDENTIFIER");
    public static IElementType INTEGER = new Token("INTEGER");
    public static IElementType STRING = new Token("STRING");
    public static IElementType SYMBOL = new Token("SYMBOL");

    private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static Pattern COMMENT_PATTERN = Pattern.compile("//[^\n]*|/\\*([^*]|\\*[^/])*(\\*/|$)");
    private static Pattern IDENTIFIER_PATTERN = Pattern.compile("\\.?[_a-zA-Z][_a-zA-Z0-9]*(\\.[_a-zA-Z][_a-zA-Z0-9]*)*");
    private static Pattern INTEGER_PATTERN = Pattern.compile("[0-9]+");
    private static Pattern STRING_PATTERN = Pattern.compile("\"[^\n\"]*\"");
    private static Pattern SYMBOL_PATTERN = Pattern.compile(",|=|;|\\{|\\}|<|>");

    private CharSequence buffer;
    private int endOffset;

    private IElementType currentToken;
    private int currentTokenStart;
    private int currentTokenEnd;

    private void computeCurrentToken() {
        if (currentTokenStart >= endOffset) {
            currentTokenEnd = endOffset;
            currentToken = null;
            return;
        }
        if (!checkCurrentToken(WHITESPACE_PATTERN, TokenType.WHITE_SPACE) &&
            !checkCurrentToken(COMMENT_PATTERN, COMMENT) &&
            !checkCurrentToken(IDENTIFIER_PATTERN, IDENTIFIER) &&
            !checkCurrentToken(INTEGER_PATTERN, INTEGER) &&
            !checkCurrentToken(STRING_PATTERN, STRING) &&
            !checkCurrentToken(SYMBOL_PATTERN, SYMBOL)) {
            currentTokenEnd = 1 + currentTokenStart;
            currentToken = TokenType.BAD_CHARACTER;
        }
    }

    private boolean checkCurrentToken(Pattern pattern, IElementType token) {
        Matcher matcher = pattern.matcher(buffer);
        matcher.region(currentTokenStart, endOffset);
        if (matcher.lookingAt()) {
            currentTokenEnd = matcher.end();
            currentToken = token;
            return true;
        }
        return false;
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.endOffset = endOffset;
        currentTokenStart = startOffset;
        computeCurrentToken();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        return currentToken;
    }

    @Override
    public int getTokenStart() {
        return currentTokenStart;
    }

    @Override
    public int getTokenEnd() {
        return currentTokenEnd;
    }

    @Override
    public void advance() {
        currentTokenStart = currentTokenEnd;
        computeCurrentToken();
    }

    @Override
    public LexerPosition getCurrentPosition() {
        return new LexerPosition() {
            @Override
            public int getOffset() {
                return currentTokenStart;
            }

            @Override
            public int getState() {
                return 0;
            }
        };
    }

    @Override
    public void restore(LexerPosition lexerPosition) {
        currentTokenStart = lexerPosition.getOffset();
    }

    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return Math.min(endOffset, buffer.length());
    }
}
