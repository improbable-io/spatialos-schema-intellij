package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SchemaPsiElement extends ASTWrapperPsiElement {
    private static final List<IElementType> ACCEPTED_REFS = Arrays.asList(
            SchemaParser.FIELD_ENUM_OR_INSTANCE,
            SchemaParser.FIELD_NEWINSTANCE_NAME,
            SchemaParser.TYPE_NAME_REFERENCE,
            SchemaParser.FIELD_TYPE,
            SchemaParser.FIELD_REFERNCE,
            SchemaParser.TYPE_PARAMETER_NAME
    );

    public SchemaPsiElement(@NotNull ASTNode node) {
        super(node);
    }


    @Override
    public PsiReference getReference() {
        if(ACCEPTED_REFS.contains(this.getNode().getElementType())) {
            return new SchemaReference(this, false);
        }
        return null;
    }

    @Override
    public String getName() {
        return this.getText();
    }

}
