package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;

public class SchemaSyntaxHighlighter extends SyntaxHighlighterBase {
    public static SchemaSyntaxHighlighter SCHEMA_SYNTAX_HIGHLIGHTER = new SchemaSyntaxHighlighter();

    @Override
    public Lexer getHighlightingLexer() {
        return SchemaLexer.SCHEMA_LEXER;
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType iElementType) {
        return new TextAttributesKey[0];
    }
}
