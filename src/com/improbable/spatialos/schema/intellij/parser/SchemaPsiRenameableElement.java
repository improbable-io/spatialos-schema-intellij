package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemaPsiRenameableElement extends SchemaPsiElement implements PsiNameIdentifierOwner {
    public SchemaPsiRenameableElement(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public PsiElement setName(@NotNull String s) throws IncorrectOperationException {
        PsiElement element = this.getNameIdentifier();
        if(element == null) {
            return this;
        }

        if(this.getNode().getElementType() == SchemaParser.TYPE_DEFINITION) {
            return SchemaElementManipulator.rename(element, "type " + s + " {}", e -> e.getChildren()[0].getChildren()[1]);
        }

        if(this.getNode().getElementType() == SchemaParser.FIELD_NAME) {
            return SchemaElementManipulator.rename(element, "type Dummy { float " + s + " = 1; }", e -> e.getChildren()[0].getChildren()[2].getChildren()[1]);
        }

        return this;
    }
}
