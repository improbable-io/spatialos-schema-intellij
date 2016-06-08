package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SchemaSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final SchemaSyntaxHighlighter SCHEMA_SYNTAX_HIGHLIGHTER = new SchemaSyntaxHighlighter();

    private static final TextAttributesKey[] BAD_CHARACTER = {HighlighterColors.BAD_CHARACTER};
    private static final TextAttributesKey[] COMMENT = {DefaultLanguageHighlighterColors.LINE_COMMENT};
    private static final TextAttributesKey[] INTEGER = {DefaultLanguageHighlighterColors.NUMBER};
    private static final TextAttributesKey[] STRING = {DefaultLanguageHighlighterColors.STRING};
    private static final TextAttributesKey[] SYMBOL = {DefaultLanguageHighlighterColors.BRACES};
    private static final TextAttributesKey[] NONE = {HighlighterColors.TEXT};

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return SchemaLexer.SCHEMA_LEXER;
    }

    @Override
    public @NotNull TextAttributesKey[] getTokenHighlights(IElementType element) {
        if (element == TokenType.BAD_CHARACTER) {
            return BAD_CHARACTER;
        }
        if (element == SchemaLexer.COMMENT) {
            return COMMENT;
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
