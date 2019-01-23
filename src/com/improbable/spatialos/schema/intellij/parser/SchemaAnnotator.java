package com.improbable.spatialos.schema.intellij.parser;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SchemaAnnotator implements Annotator {
    private static final List<String> OPTION_VALUES = Arrays.asList("true", "false");
    private static final List<String> BUILT_IN_GENERIC_TYPES = Arrays.asList("option", "list", "map");
    private static final List<String> BUILT_IN_TYPES = Arrays.asList(
            "double", "float", "string", "bytes", "int32", "int64", "uint32", "uint64", "sint32", "sint64", "fixed32",
            "fixed64", "sfixed32", "sfixed64", "bool"
    );

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element.getNode().getElementType() == SchemaParser.KEYWORD) {
            highlight(holder, element, DefaultLanguageHighlighterColors.KEYWORD);
        }
        if (element.getNode().getElementType() == SchemaParser.OPTION_VALUE &&
            OPTION_VALUES.contains(element.getText())) {
            highlight(holder, element, DefaultLanguageHighlighterColors.NUMBER);
        }
        if (element.getNode().getElementType() == SchemaParser.TYPE_NAME &&
            BUILT_IN_GENERIC_TYPES.contains(element.getText())) {
            highlight(holder, element, DefaultLanguageHighlighterColors.KEYWORD);
        }
        if (element.getNode().getElementType() == SchemaParser.TYPE_PARAMETER_NAME ||
             element.getNode().getElementType() == SchemaParser.TYPE_NAME) {
            if (BUILT_IN_TYPES.contains(element.getText())) {
                highlight(holder, element, DefaultLanguageHighlighterColors.KEYWORD);
            } else {
                highlight(holder, element, DefaultLanguageHighlighterColors.METADATA);
            }
        }
        if(element.getNode().getElementType() == SchemaParser.COMMAND_NAME) {
            highlight(holder, element, DefaultLanguageHighlighterColors.INSTANCE_METHOD);
        }
        if(element.getNode().getText().equals("{") || element.getNode().getText().equals("}") ||
           element.getNode().getText().equals("[") || element.getNode().getText().equals("]")) {
            highlight(holder, element, DefaultLanguageHighlighterColors.BRACKETS);
        }
        if(element.getNode().getText().equals("(") || element.getNode().getText().equals(")")) {
            highlight(holder, element, DefaultLanguageHighlighterColors.PARENTHESES);
        }
        if(element.getNode().getText().equals(",")) {
            highlight(holder, element, DefaultLanguageHighlighterColors.COMMA);
        }
        if(element.getNode().getText().equals(";")) {
            highlight(holder, element, DefaultLanguageHighlighterColors.SEMICOLON);
        }
    }

    private void highlight(@NotNull AnnotationHolder holder, @NotNull PsiElement element,
                           @NotNull TextAttributesKey attributes) {
        holder.createInfoAnnotation(element, "").setTextAttributes(attributes);
    }
}