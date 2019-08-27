package com.improbable.spatialos.schema.intellij.actions;

import com.improbable.spatialos.schema.intellij.parser.SchemaLexer;
import com.improbable.spatialos.schema.intellij.parser.SchemaParser;
import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SchemaBlock implements Block {
    private static final List<IElementType> INDENT_BLOCKS = Arrays.asList(
        SchemaParser.ENUM_DEFINITION, SchemaParser.TYPE_DEFINITION, SchemaParser.COMPONENT_DEFINITION);
    private static final List<IElementType> CONTINUATION_BLOCKS = Arrays.asList(
        SchemaParser.PACKAGE_DEFINITION, SchemaParser.IMPORT_DEFINITION,
        SchemaParser.FIELD_DEFINITION, SchemaParser.DATA_DEFINITION, SchemaParser.EVENT_DEFINITION,
        SchemaParser.FIELD_TYPE, SchemaParser.ENUM_VALUE_DEFINITION, SchemaParser.COMPONENT_ID_DEFINITION);

    private static final Spacing NO_SPACING = Spacing.createSpacing(0, 0, 0, false, 0);
    private static final Spacing ONE_SPACE = Spacing.createSpacing(1, 1, 0, false, 0);
    private static final Spacing SPACE_OR_BREAK = Spacing.createSpacing(0, 1, 0, true, 0);
    private static final Spacing NO_SPACE_OR_BREAK = Spacing.createSpacing(0, 0, 0, true, 0);
    private static final Spacing ONE_BREAK = Spacing.createSpacing(0, 0, 1, false, 0);
    private static final Spacing FREE_BREAKS = Spacing.createSpacing(0, 0, 1, true, 1);
    private static final Spacing FREE_BREAKS_AND_SPACES = Spacing.createSpacing(1, Integer.MAX_VALUE, 0, true, 1);

    private final ASTNode node;
    private final Indent indent;
    private final List<SchemaBlock> subBlocks = new ArrayList<>();

    public SchemaBlock(@NotNull ASTNode node, @NotNull Indent indent) {
        this.node = node;
        this.indent = indent;

        for (ASTNode child : node.getChildren(null)) {
            if (child.getText().trim().length() > 0) {
                Indent childIndent = getIndentForChild(subBlocks.size(), child.getElementType());
                subBlocks.add(new SchemaBlock(child, childIndent));
            }
        }
    }

    @Override
    public @NotNull TextRange getTextRange() {
        return node.getTextRange();
    }

    @Override
    public @NotNull List<Block> getSubBlocks() {
        return Collections.unmodifiableList(subBlocks);
    }

    @Override
    public @Nullable Wrap getWrap() {
        return null;
    }

    @Override
    public @Nullable Indent getIndent() {
        return indent;
    }

    @Override
    public @Nullable Alignment getAlignment() {
        return null;
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        if (!(child1 instanceof SchemaBlock) || !(child2 instanceof SchemaBlock)) {
            return null;
        }
        SchemaBlock left = (SchemaBlock) child1;
        SchemaBlock right = (SchemaBlock) child2;

        // Comments.
        if (left.node.getPsi() instanceof PsiComment || right.node.getPsi() instanceof PsiComment) {
            return FREE_BREAKS_AND_SPACES;
        }

        // Braces.
        if (left.node.getPsi().getText().equals("{") || right.node.getPsi().getText().equals("}")) {
            return ONE_BREAK;
        }
        if (left.node.getPsi().getText().equals("}")) {
            return FREE_BREAKS;
        }
        if (right.node.getPsi().getText().equals("{")) {
            return ONE_SPACE;
        }

        // Angle brackets.
        if (left.node.getPsi().getText().equals("<") || right.node.getPsi().getText().equals(">")) {
            return NO_SPACE_OR_BREAK;
        }
        if (left.node.getPsi().getText().equals(">")) {
            return SPACE_OR_BREAK;
        }
        if (right.node.getPsi().getText().equals("<")) {
            return NO_SPACING;
        }

        // Comma and semicolon.
        if (right.node.getPsi().getText().equals(",") || right.node.getPsi().getText().equals(";")) {
            return NO_SPACING;
        }
        if (left.node.getPsi().getText().equals(",")) {
            return SPACE_OR_BREAK;
        }
        if (left.node.getPsi().getText().equals(";")) {
            return FREE_BREAKS;
        }

        // Equals.
        if (left.node.getPsi().getText().equals("=") || right.node.getPsi().getText().equals("=")) {
            return SPACE_OR_BREAK;
        }

        return SPACE_OR_BREAK;
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(getIndentForChild(newChildIndex, null), null);
    }

    @Override
    public boolean isIncomplete() {
        IElementType element = node.getElementType();
        IElementType lastElement =
            subBlocks.isEmpty() ? null : subBlocks.get(subBlocks.size() - 1).node.getElementType();
        return
            (INDENT_BLOCKS.contains(element) && lastElement != SchemaLexer.RBRACE) ||
            (CONTINUATION_BLOCKS.contains(element) && lastElement != SchemaLexer.SEMICOLON);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    private Indent getIndentForChild(int newChildIndex, @Nullable IElementType newElement) {
        IElementType element = node.getElementType();
        if (INDENT_BLOCKS.contains(element)) {
            boolean afterLeftBrace = false;
            boolean afterRightBrace = false;
            for (int i = 0; i < newChildIndex && i < subBlocks.size(); ++i) {
                IElementType childElement = subBlocks.get(i).node.getElementType();
                afterLeftBrace |= childElement == SchemaLexer.LBRACE;
                afterRightBrace |= childElement == SchemaLexer.RBRACE;
            }
            boolean shouldIndent = afterLeftBrace && !afterRightBrace && newElement != SchemaLexer.RBRACE;
            return shouldIndent ? Indent.getNormalIndent() : Indent.getNoneIndent();
        }
        // Doesn't indent with continuation on pressing enter after the first element in an incomplete line. I can't
        // figure out why.
        return CONTINUATION_BLOCKS.contains(element) ?
            Indent.getContinuationWithoutFirstIndent() : Indent.getNoneIndent();
    }
}
