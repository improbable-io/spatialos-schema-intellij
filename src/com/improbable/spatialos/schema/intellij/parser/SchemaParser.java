package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SchemaParser implements PsiParser {
    public static SchemaParser SCHEMA_PARSER = new SchemaParser();

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        return builder.getTreeBuilt();
    }
}
