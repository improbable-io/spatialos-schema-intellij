package com.improbable.spatialos.schema.intellij.parser;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.hints.HintInfo;
import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.psi.PsiElement;
import org.apache.commons.compress.utils.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SchemaParameterHintsProvider implements InlayParameterHintsProvider {
    @NotNull
    @Override
    public List<InlayInfo> getParameterHints(PsiElement psiElement) {
        if(psiElement.getParent() != null && psiElement.getParent().getNode() != null && psiElement.getParent().getNode().getElementType() == SchemaParser.FIELD_ARRAY && psiElement.getNode().getElementType() instanceof SchemaParser.Node) {
            PsiElement parent = psiElement.getParent().getParent();
            String clazzName = "";
            int index = -1;
            if(parent.getNode().getElementType() == SchemaParser.ANNOTATION_DEFINITION || parent.getNode().getElementType() == SchemaParser.FIELD_NEWINSTANCE) {
                PsiElement[] siblings = parent.getChildren();
                if(siblings.length > 1) {
                    if(siblings[0].getNode().getElementType() == (parent.getNode().getElementType() == SchemaParser.ANNOTATION_DEFINITION ? SchemaParser.TYPE_NAME_REFERENCE : SchemaParser.FIELD_NEWINSTANCE_NAME)) {
                        clazzName = siblings[0].getText();
                    }
                    PsiElement[] childSiblings = siblings[1].getChildren();
                    for (int i = 0; i < childSiblings.length; i++) {
                        if(childSiblings[i] == psiElement) {
                            index = i;
                        }
                    }
                }
                PsiElement element = SchemaAnnotator.resolveElement(parent, clazzName);
                if(element != null) {
                    int found = 0;
                    for (PsiElement child : element.getChildren()) {
                        if(child.getNode().getElementType() == SchemaParser.FIELD_DEFINITION) {
                            if(found++ == index) {
                                PsiElement[] fieldElement = child.getChildren();
                                if(fieldElement.length > 1) {
                                    return Lists.newArrayList(new InlayInfo(fieldElement[1].getText(), psiElement.getTextRange().getStartOffset()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public HintInfo getHintInfo(PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public Set<String> getDefaultBlackList() {
        return Sets.newHashSet();// TODO
    }
}
