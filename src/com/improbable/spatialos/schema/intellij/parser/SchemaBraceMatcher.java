package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemaBraceMatcher implements PairedBraceMatcher {
    private static BracePair[] BRACE_PAIRS = {new BracePair(SchemaLexer.LBRACE, SchemaLexer.RBRACE, true),
                                              new BracePair(SchemaLexer.LANGLE, SchemaLexer.RANGLE, false)};

    @Override
    public BracePair[] getPairs() {
        return BRACE_PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType,
                                                   @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile psiFile, int i) {
        return Math.max(0, i - 1);
    }
}
