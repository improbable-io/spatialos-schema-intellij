package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;

public class SchemaLexer extends Lexer {
    public static SchemaLexer SCHEMA_LEXER = new SchemaLexer();

    @Override
    public void start(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        return null;
    }

    @Override
    public int getTokenStart() {
        return 0;
    }

    @Override
    public int getTokenEnd() {
        return 0;
    }

    @Override
    public void advance() {

    }

    @Override
    public LexerPosition getCurrentPosition() {
        return null;
    }

    @Override
    public void restore(LexerPosition lexerPosition) {

    }

    @Override
    public CharSequence getBufferSequence() {
        return null;
    }

    @Override
    public int getBufferEnd() {
        return 0;
    }
}
