package com.improbable.spatialos.schema.intellij.actions;

import com.improbable.spatialos.schema.intellij.parser.SchemaLexer;
import com.improbable.spatialos.schema.intellij.parser.SchemaParser;
import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleSettings;
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
        SchemaParser.PACKAGE_DEFINITION, SchemaParser.IMPORT_DEFINITION, SchemaParser.OPTION_DEFINITION,
        SchemaParser.FIELD_DEFINITION, SchemaParser.FIELD_TYPE,
        SchemaParser.ENUM_VALUE_DEFINITION, SchemaParser.COMPONENT_ID_DEFINITION);

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
        return null;
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(getIndentForChild(newChildIndex, null), null);
    }

    @Override
    public boolean isIncomplete() {
        return node.getElementType() == SchemaParser.INCOMPLETE;
    }

    @Override
    public boolean isLeaf() {
        return node.getFirstChildNode() == null;
    }

    private Indent getIndentForChild(int newChildIndex, @Nullable IElementType newElement) {
        if (CONTINUATION_BLOCKS.contains(node.getElementType())) {
            return newChildIndex == 0 ? Indent.getNoneIndent() : Indent.getContinuationIndent();
        }
        if (INDENT_BLOCKS.contains(node.getElementType())) {
            boolean afterLeftBrace = false;
            boolean afterRightBrace = false;
            for (int i = 0; i < newChildIndex && i < subBlocks.size(); ++i) {
                IElementType element = subBlocks.get(i).node.getElementType();
                afterLeftBrace |= element == SchemaLexer.LBRACE;
                afterRightBrace |= element == SchemaLexer.RBRACE;
            }
            boolean shouldIndent = afterLeftBrace && !afterRightBrace && newElement != SchemaLexer.RBRACE;
            return shouldIndent ? Indent.getNormalIndent() : Indent.getNoneIndent();
        }
        return Indent.getNoneIndent();
    }
}
