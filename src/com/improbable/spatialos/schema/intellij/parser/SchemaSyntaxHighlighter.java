package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public class SchemaSyntaxHighlighter extends SyntaxHighlighterBase {
    public static SchemaSyntaxHighlighter SCHEMA_SYNTAX_HIGHLIGHTER = new SchemaSyntaxHighlighter();

    private static TextAttributesKey[] BAD_CHARACTER = {HighlighterColors.BAD_CHARACTER};
    private static TextAttributesKey[] WHITESPACE = {HighlighterColors.TEXT};
    private static TextAttributesKey[] IDENTIFIER = {DefaultLanguageHighlighterColors.CLASS_NAME};
    private static TextAttributesKey[] INTEGER = {DefaultLanguageHighlighterColors.NUMBER};
    private static TextAttributesKey[] STRING = {DefaultLanguageHighlighterColors.STRING};
    private static TextAttributesKey[] SYMBOL = {DefaultLanguageHighlighterColors.BRACES};
    private static TextAttributesKey[] NONE = {};

    @Override
    public Lexer getHighlightingLexer() {
        return SchemaLexer.SCHEMA_LEXER;
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType element) {
        if (element == TokenType.BAD_CHARACTER) {
            return BAD_CHARACTER;
        }
        if (element == TokenType.WHITE_SPACE) {
            return WHITESPACE;
        }
        if (element == SchemaLexer.IDENTIFIER) {
            return IDENTIFIER;
        }
        if (element == SchemaLexer.INTEGER) {
            return INTEGER;
        }
        if (element == SchemaLexer.STRING) {
            return STRING;
        }
        if (element == SchemaLexer.SYMBOL) {
            return SYMBOL;
        }
        return NONE;
    }
}
