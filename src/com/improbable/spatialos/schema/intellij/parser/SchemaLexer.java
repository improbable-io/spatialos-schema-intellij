package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemaLexer extends Lexer {
    public static final SchemaLexer SCHEMA_LEXER = new SchemaLexer();
    public static final IElementType COMMENT = new Token("Comment");
    public static final IElementType IDENTIFIER = new Token("Identifier");
    public static final IElementType INTEGER = new Token("Integer");
    public static final IElementType BOOLEAN = new Token("Boolean");
    public static final IElementType STRING = new Token("String");
    public static final IElementType SYMBOL = new Token("Symbol");
    public static final IElementType LBRACE = new Token("{");
    public static final IElementType RBRACE = new Token("}");
    public static final IElementType LPARENTHESES = new Token("(");
    public static final IElementType RPARENTHESES = new Token(")");
    public static final IElementType LBRACKET = new Token("[");
    public static final IElementType RBRACKET = new Token("]");
    public static final IElementType LANGLE = new Token("<");
    public static final IElementType RANGLE = new Token(">");
    public static final IElementType EQUALS = new Token("=");
    public static final IElementType COMMA = new Token(",");
    public static final IElementType COLON = new Token(":");
    public static final IElementType SEMICOLON = new Token(";");

    private static final String IDENTIFIER_PATTERN_STR = "[_a-zA-Z][_a-zA-Z0-9]*(\\.([_a-zA-Z][_a-zA-Z0-9]*)?)*";
    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("\\.(" + IDENTIFIER_PATTERN_STR + ")?|" + IDENTIFIER_PATTERN_STR);

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//[^\n]*|/\\*([^*]|\\*([^/]|$))*(\\*/|$)");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("[0-9]+");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("(?i)(?:true|false)");
    private static final Pattern STRING_PATTERN = Pattern.compile("\".*?\"");
    private static final Pattern LBRACE_PATTERN = Pattern.compile("\\{");
    private static final Pattern RBRACE_PATTERN = Pattern.compile("}");
    private static final Pattern LPARENTHESES_PATTERN = Pattern.compile("\\(");
    private static final Pattern RPARENTHESES_PATTERN = Pattern.compile("\\)");
    private static final Pattern LBRACKET_PATTERN = Pattern.compile("\\[");
    private static final Pattern RBRACKET_PATTERN = Pattern.compile("]");
    private static final Pattern LANGLE_PATTERN = Pattern.compile("<");
    private static final Pattern RANGLE_PATTERN = Pattern.compile(">");
    private static final Pattern EQUALS_PATTERN = Pattern.compile("=");
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final Pattern COLON_PATTERN = Pattern.compile(":");
    private static final Pattern SEMICOLON_PATTERN = Pattern.compile(";");

    private static class Token extends IElementType {
        public Token(String debugName) {
            super(debugName, SchemaLanguage.SCHEMA_LANGUAGE);
        }
    }

    private CharSequence buffer = null;
    private int endOffset = 0;

    private IElementType currentToken = null;
    private int currentTokenStart = 0;
    private int currentTokenEnd = 0;

    private void computeCurrentToken() {
        if (currentTokenStart >= endOffset) {
            currentTokenEnd = endOffset;
            currentToken = null;
            return;
        }
        if (!checkCurrentToken(WHITESPACE_PATTERN, TokenType.WHITE_SPACE) &&
                !checkCurrentToken(COMMENT_PATTERN, COMMENT) &&
                !checkCurrentToken(BOOLEAN_PATTERN, BOOLEAN) &&
                !checkCurrentToken(IDENTIFIER_PATTERN, IDENTIFIER) &&
                !checkCurrentToken(INTEGER_PATTERN, INTEGER) &&
                !checkCurrentToken(STRING_PATTERN, STRING) &&
                !checkCurrentToken(LBRACE_PATTERN, LBRACE) &&
                !checkCurrentToken(RBRACE_PATTERN, RBRACE) &&
                !checkCurrentToken(LPARENTHESES_PATTERN, LPARENTHESES) &&
                !checkCurrentToken(RPARENTHESES_PATTERN, RPARENTHESES) &&
                !checkCurrentToken(LANGLE_PATTERN, LANGLE) &&
                !checkCurrentToken(RANGLE_PATTERN, RANGLE) &&
                !checkCurrentToken(EQUALS_PATTERN, EQUALS) &&
                !checkCurrentToken(COMMA_PATTERN, COMMA) &&
                !checkCurrentToken(SEMICOLON_PATTERN, SEMICOLON) &&
                !checkCurrentToken(LBRACKET_PATTERN, LBRACKET) &&
                !checkCurrentToken(RBRACKET_PATTERN, RBRACKET) &&
                !checkCurrentToken(COLON_PATTERN, COLON)) {
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
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
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
    public @NotNull
    LexerPosition getCurrentPosition() {
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
    public void restore(@NotNull LexerPosition lexerPosition) {
        currentTokenStart = lexerPosition.getOffset();
    }

    @Override
    public @NotNull
    CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return Math.min(endOffset, buffer.length());
    }
}
