package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaFileType;
import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class SchemaParserDefinition implements ParserDefinition {
    private static IFileElementType SCHEMA_FILE = new IFileElementType(SchemaLanguage.SCHEMA_LANGUAGE);
    private static TokenSet WHITESPACE_TOKENS = TokenSet.create(TokenType.WHITE_SPACE);
    private static TokenSet COMMENT_TOKENS = TokenSet.create(SchemaLexer.COMMENT);
    private static TokenSet STRING_TOKENS = TokenSet.create(SchemaLexer.STRING);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return SchemaLexer.SCHEMA_LEXER;
    }

    @Override
    public PsiParser createParser(Project project) {
        return SchemaParser.SCHEMA_PARSER;
    }

    @Override
    public IFileElementType getFileNodeType() {
        return SCHEMA_FILE;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITESPACE_TOKENS;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENT_TOKENS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRING_TOKENS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode astNode) {
        return null;
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new PsiFileBase(fileViewProvider, SchemaLanguage.SCHEMA_LANGUAGE) {
            @Override
            public @NotNull FileType getFileType() {
                return SchemaFileType.SCHEMA_FILE_TYPE;
            }
        };
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        if ((left == SchemaLexer.IDENTIFIER || left == SchemaLexer.INTEGER) &&
            (right == SchemaLexer.IDENTIFIER || right == SchemaLexer.INTEGER)) {
            return SpaceRequirements.MUST;
        }
        return SpaceRequirements.MAY;
    }
}
