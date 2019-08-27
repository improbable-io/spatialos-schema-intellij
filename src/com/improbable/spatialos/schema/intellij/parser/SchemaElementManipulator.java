package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaFileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemaElementManipulator implements ElementManipulator<PsiElement> {

    @Nullable
    @Override
    public PsiElement handleContentChange(@NotNull PsiElement psiElement, @NotNull TextRange textRange, String s) throws IncorrectOperationException {
        if(psiElement.getNode().getElementType() == SchemaParser.FIELD_TYPE) {
            PsiElement child = psiElement.getChildren()[0];
            return rename(child, textRange.replace(child.getText(), "type Dummy { " + getPrefix(child) + s + " dummy = 1; }"), element -> element.getChildren()[0].getChildren()[2].getChildren()[0]);
        }
        if(psiElement.getNode().getElementType() == SchemaParser.TYPE_PARAMETER_NAME) {
            return rename(psiElement, textRange.replace(psiElement.getText(), "type Dummy { " + getPrefix(psiElement) +  s + " dummy = 1; }"), element -> element.getChildren()[0].getChildren()[2].getChildren()[0]);
        }
        return null;
    }

    private static String getPrefix(@NotNull PsiElement element) {
        PsiElement resolved = SchemaAnnotator.resolveElement(element, element.getText());
        if(resolved != null) {
            if(resolved.getContainingFile() == element.getContainingFile()) { //Same file
                return "";
            }
            for (PsiElement child : resolved.getContainingFile().getChildren()) {
                if(child.getNode().getElementType() == SchemaParser.PACKAGE_DEFINITION) {
                    return child.getChildren()[1].getText() + ".";
                }
            }

        }
        return "";
    }

    @Nullable
    @Override
    public PsiElement handleContentChange(@NotNull PsiElement psiElement, String s) throws IncorrectOperationException {
        return null;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull PsiElement psiElement) {
        return TextRange.from(0, psiElement.getTextLength());
    }

    public static PsiElement rename(PsiElement element, String newText, Function<PsiElement, PsiElement> func) {
        return element.replace(func.fun(PsiFileFactory.getInstance(element.getProject()).createFileFromText("DUMMY.schema", SchemaFileType.SCHEMA_FILE_TYPE, newText)));
    }

}
