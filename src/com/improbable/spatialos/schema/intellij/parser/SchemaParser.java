package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

public class SchemaParser implements PsiParser {
    public static SchemaParser SCHEMA_PARSER = new SchemaParser();

    @Override
    public ASTNode parse(IElementType iElementType, PsiBuilder psiBuilder) {
        return null;
    }
}
