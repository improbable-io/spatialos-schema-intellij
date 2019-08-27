package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemaReference extends PsiReferenceBase<PsiElement> {

    public SchemaReference(@NotNull PsiElement element, boolean soft) {
        super(element, soft);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        if(this.myElement.getNode().getElementType() == SchemaParser.FIELD_REFERNCE) {
            PsiElement type = SchemaAnnotator.resolveElement(this.myElement, this.myElement.getParent().getParent().getChildren()[0].getText() + "." + this.myElement.getText());
            if(type != null) {
                return type.getChildren()[1];
            }
            return null;
        }
        if(this.myElement.getNode().getElementType() == SchemaParser.TYPE_NAME_REFERENCE || this.myElement.getNode().getElementType() == SchemaParser.FIELD_TYPE || this.myElement.getNode().getElementType() == SchemaParser.TYPE_PARAMETER_NAME) {
            PsiElement type =  SchemaAnnotator.resolveElement(this.myElement, this.myElement.getText());
            if(type != null) {
                return type.getChildren()[1];
            }
            return null;
        }
        if(this.myElement.getNode().getElementType() == SchemaParser.FIELD_NEWINSTANCE_NAME || this.myElement.getNode().getElementType() == SchemaParser.FIELD_ENUM_OR_INSTANCE) {
            return SchemaAnnotator.resolveElement(this.myElement, this.myElement.getText());
        }
        return null;
    }


    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
