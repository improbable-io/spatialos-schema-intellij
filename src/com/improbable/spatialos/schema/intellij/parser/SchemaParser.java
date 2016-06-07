package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemaParser implements PsiParser {
    public static SchemaParser SCHEMA_PARSER = new SchemaParser();

    public static String KEYWORD_PACKAGE = "package";
    public static String KEYWORD_IMPORT = "import";
    public static String KEYWORD_ENUM = "enum";
    public static String KEYWORD_TYPE = "type";
    public static String KEYWORD_COMPONENT = "component";
    public static String KEYWORD_OPTION = "option";
    public static String KEYWORD_ID = "id";

    public static IElementType PACKAGE_DEFINITION = new Node("package definition");
    public static IElementType IMPORT_DEFINITION = new Node("import definition");
    public static IElementType OPTION_DEFINITION = new Node("option definition");
    public static IElementType FIELD_DEFINITION = new Node("field definition");
    public static IElementType COMPONENT_DEFINITION = new Node("component definition");
    public static IElementType COMPONENT_ID_DEFINITION = new Node("component id definition");
    public static IElementType ENUM_DEFINITION = new Node("enum definition");
    public static IElementType ENUM_VALUE_DEFINITION = new Node("enum value definition");
    public static IElementType TYPE_DEFINITION = new Node("type definition");
    public static IElementType INCOMPLETE = new Node("incomplete");

    private static class Node extends IElementType {
        public Node(String debugName) {
            super(debugName, SchemaLanguage.SCHEMA_LANGUAGE);
        }
    }

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        Instance instance = new Instance(builder);
        instance.parseSchemaFile(root);
        return builder.getTreeBuilt();
    }

    private static class Instance {
        private PsiBuilder builder;
        private enum Construct {
            STATEMENT,
            BRACES,
            TOP_LEVEL,
        }

        public Instance(@NotNull PsiBuilder builder) {
            this.builder = builder;
        }

        private void error(@Nullable PsiBuilder.Marker marker, Construct construct, String s, Object... args) {
            if (marker != null) {
                marker.done(INCOMPLETE);
            }
            String errorMessage = String.format(s, args);
            PsiBuilder.Marker errorMarker = builder.mark();

            while (builder.getTokenType() != null && !builder.eof()) {
                if ((construct == Construct.STATEMENT || construct == Construct.TOP_LEVEL) && isSymbol(';')) {
                    errorMarker.error(errorMessage);
                    builder.advanceLexer();
                    return;
                }
                if ((construct == Construct.BRACES || construct == Construct.TOP_LEVEL) && isSymbol('}')) {
                    errorMarker.error(errorMessage);
                    builder.advanceLexer();
                    return;
                }
                if (construct == Construct.STATEMENT && isSymbol('}')) {
                    errorMarker.error(errorMessage);
                    return;
                }
                builder.advanceLexer();
            }
            errorMarker.error(errorMessage);
        }

        private String getTokenText() {
            return builder.getTokenText() == null ? "<EOF>" : builder.getTokenText();
        }

        private String getIdentifier() {
            return builder.getTokenText() == null ? "" : builder.getTokenText();
        }

        private int getInteger() {
            if (builder.getTokenText() == null) {
                return 0;
            }
            try {
                return Integer.parseInt(builder.getTokenText());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private String getString() {
            String text = builder.getTokenText();
            return text == null ? "" : text.substring(1, text.length() - 2);
        }

        private boolean isIdentifier() {
            return builder.getTokenType() == SchemaLexer.IDENTIFIER;
        }

        private boolean isInteger() {
            return builder.getTokenType() == SchemaLexer.INTEGER;
        }

        private boolean isString() {
            return builder.getTokenType() == SchemaLexer.STRING;
        }

        private boolean isIdentifier(@NotNull String identifier) {
            return builder.getTokenType() == SchemaLexer.IDENTIFIER &&
                    builder.getTokenText() != null && builder.getTokenText().equals(identifier);
        }

        private boolean isSymbol(char c) {
            return builder.getTokenType() == SchemaLexer.SYMBOL &&
                    builder.getTokenText() != null && builder.getTokenText().equals(Character.toString(c));
        }

        private void parsePackageDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.STATEMENT, "Expected a package name after '%s'.", KEYWORD_PACKAGE);
                return;
            }
            String packageName = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol(';')) {
                error(marker, Construct.STATEMENT, "Expected ';' after %s definition.", KEYWORD_PACKAGE);
            }
            builder.advanceLexer();
            marker.done(PACKAGE_DEFINITION);
        }

        private void parseImportDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isString()) {
                error(marker, Construct.STATEMENT, "Expected a quoted filename after '%s'.", KEYWORD_IMPORT);
                return;
            }
            String filename = getString();
            builder.advanceLexer();
            if (!isSymbol(';')) {
                error(marker, Construct.STATEMENT, "Expected ';' after '%s \"%s\"'.", KEYWORD_IMPORT, filename);
                return;
            }
            builder.advanceLexer();
            marker.done(IMPORT_DEFINITION);
        }

        private void parseOptionDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.STATEMENT, "Expected identifier after '%s'.", KEYWORD_OPTION);
                return;
            }
            String name = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol('=')) {
                error(marker, Construct.STATEMENT, "Expected '=' after '%s %s'.", KEYWORD_OPTION, name);
                return;
            }
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.STATEMENT, "Expected option value after '%s %s = '.", KEYWORD_OPTION, name);
                return;
            }
            String value = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol(';')) {
                error(marker, Construct.STATEMENT, "Expected ';' after '%s %s = %s'.", KEYWORD_OPTION, name, value);
                return;
            }
            builder.advanceLexer();
            marker.done(OPTION_DEFINITION);
        }

        private @Nullable String parseTypeName(@NotNull PsiBuilder.Marker marker) {
            String name = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol('<')) {
                return name;
            }
            name = name + '<';
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.STATEMENT, "Expected typename after '%s<'.", name);
                return null;
            }
            name = name + getIdentifier();
            builder.advanceLexer();
            while (true) {
                if (isSymbol('>')) {
                    name = name + '>';
                    builder.advanceLexer();
                    return name;
                }
                if (isSymbol(',')) {
                    name = name + ", ";
                    builder.advanceLexer();
                    if (!isIdentifier()) {
                        error(marker, Construct.STATEMENT, "Expected typename after ','.");
                        return null;
                    }
                    name = name + getIdentifier();
                    builder.advanceLexer();
                    continue;
                }
                error(marker, Construct.STATEMENT, "Invalid '%s' inside <>.", getTokenText());
                return null;
            }
        }

        private void parseFieldDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            String typeName = parseTypeName(marker);
            if (typeName == null) {
                return;
            }
            if (isSymbol(';')) {
                builder.advanceLexer();
                return;
            }
            if (!isIdentifier()) {
                error(marker, Construct.STATEMENT, "Expected ';' or field name after '%s'.", typeName);
                return;
            }
            String fieldName = getIdentifier();
            builder.advanceLexer();
            if (isSymbol(';')) {
                builder.advanceLexer();
                return;
            }
            if (!isSymbol('=')) {
                error(marker, Construct.STATEMENT, "Expected ';' or '=' after '%s %s'.", typeName, fieldName);
                return;
            }
            builder.advanceLexer();
            if (!isInteger()) {
                error(marker, Construct.STATEMENT, "Expected field number after '%s %s = '.", typeName, fieldName);
                return;
            }
            int fieldNumber = getInteger();
            builder.advanceLexer();
            if (!isSymbol(';')) {
                error(marker, Construct.STATEMENT, "Expected ';' after '%s %s = %d'.",
                      typeName, fieldName, fieldNumber);
                return;
            }
            builder.advanceLexer();
            marker.done(FIELD_DEFINITION);
        }

        private void parseEnumContents() {
            while (isIdentifier()) {
                PsiBuilder.Marker marker = builder.mark();
                String name = getIdentifier();
                builder.advanceLexer();
                if (!isSymbol('=')) {
                    error(marker, Construct.STATEMENT, "Expected '=' after '%s'.", name);
                    continue;
                }
                builder.advanceLexer();
                if (!isInteger()) {
                    error(marker, Construct.STATEMENT, "Expected integer enum value after '%s = '.", name);
                    continue;
                }
                int value = getInteger();
                builder.advanceLexer();
                if (!isSymbol(';')) {
                    error(marker, Construct.STATEMENT, "Expected ';' after '%s = %d'.", name, value);
                    continue;
                }
                builder.advanceLexer();
                marker.done(ENUM_VALUE_DEFINITION);
            }
        }

        private void parseTypeContents() {
            while (true) {
                if (isIdentifier(KEYWORD_OPTION)) {
                    PsiBuilder.Marker marker = builder.mark();
                    builder.advanceLexer();
                    boolean lookaheadIsOption = !isSymbol('<');
                    marker.rollbackTo();
                    if (lookaheadIsOption) {
                        parseOptionDefinition();
                        continue;
                    }
                }
                if (isIdentifier(KEYWORD_ENUM)) {
                    parseEnumDefinition();
                    continue;
                }
                if (isIdentifier(KEYWORD_TYPE)) {
                    parseTypeDefinition();
                    continue;
                }
                if (isIdentifier()) {
                    parseFieldDefinition();
                    continue;
                }
                return;
            }
        }

        private void parseComponentIdDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isSymbol('=')) {
                error(marker, Construct.STATEMENT, "Expected '=' after '%s'.", KEYWORD_ID);
                return;
            }
            builder.advanceLexer();
            if (!isInteger()) {
                error(marker, Construct.STATEMENT, "Expected integer ID value after '%s = '.", KEYWORD_ID);
                return;
            }
            int value = getInteger();
            builder.advanceLexer();
            if (!isSymbol(';')) {
                error(marker, Construct.STATEMENT, "Expected ';' after '%s = %d'.", KEYWORD_ID, value);
                return;
            }
            builder.advanceLexer();
            marker.done(COMPONENT_ID_DEFINITION);
        }

        private void parseComponentContents() {
            while (true) {
                if (isIdentifier(KEYWORD_OPTION)) {
                    PsiBuilder.Marker marker = builder.mark();
                    builder.advanceLexer();
                    boolean lookaheadIsOption = !isSymbol('<');
                    marker.rollbackTo();
                    if (lookaheadIsOption) {
                        parseOptionDefinition();
                        continue;
                    }
                }
                if (isIdentifier(KEYWORD_ID)) {
                    parseComponentIdDefinition();
                    continue;
                }
                if (isIdentifier()) {
                    parseFieldDefinition();
                    continue;
                }
                return;
            }
        }

        private void parseEnumDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.BRACES, "Expected identifier after '%s'.", KEYWORD_ENUM);
                return;
            }
            String name = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol('{')) {
                error(marker, Construct.BRACES, "Expected '{' after '%s %s'.", KEYWORD_ENUM, name);
                return;
            }
            builder.advanceLexer();
            parseEnumContents();
            if (!isSymbol('}')) {
                error(marker, Construct.BRACES, "Invalid '%s' inside %s %s.", getTokenText(), KEYWORD_ENUM, name);
                return;
            }
            builder.advanceLexer();
            marker.done(ENUM_DEFINITION);
        }

        private void parseTypeDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.BRACES, "Expected identifier after '%s'.", KEYWORD_TYPE);
                return;
            }
            String name = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol('{')) {
                error(marker, Construct.BRACES, "Expected '{' after '%s %s'.", KEYWORD_TYPE, name);
                return;
            }
            builder.advanceLexer();
            parseTypeContents();
            if (!isSymbol('}')) {
                error(marker, Construct.BRACES, "Invalid '%s' inside %s %s.", getTokenText(), KEYWORD_TYPE, name);
                return;
            }
            builder.advanceLexer();
            marker.done(TYPE_DEFINITION);
        }

        private void parseComponentDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            if (!isIdentifier()) {
                error(marker, Construct.BRACES, "Expected identifier after '%s'.", KEYWORD_COMPONENT);
                return;
            }
            String name = getIdentifier();
            builder.advanceLexer();
            if (!isSymbol('{')) {
                error(marker, Construct.BRACES, "Expected '{' after '%s %s'.", KEYWORD_COMPONENT, name);
                return;
            }
            builder.advanceLexer();
            parseComponentContents();
            if (!isSymbol('}')) {
                error(marker, Construct.BRACES, "Invalid '%s' inside %s %s.", getTokenText(), KEYWORD_COMPONENT, name);
                return;
            }
            builder.advanceLexer();
            marker.done(COMPONENT_DEFINITION);
        }

        private void parseTopLevelDefinition() {
            if (isIdentifier(KEYWORD_PACKAGE)) {
                parsePackageDefinition();
            } else if (isIdentifier(KEYWORD_IMPORT)) {
                parseImportDefinition();
            } else if (isIdentifier(KEYWORD_ENUM)) {
                parseEnumDefinition();
            } else if (isIdentifier(KEYWORD_TYPE)) {
                parseTypeDefinition();
            } else if (isIdentifier(KEYWORD_COMPONENT)) {
                parseComponentDefinition();
            } else {
                error(null, Construct.TOP_LEVEL, "Expected '%s', '%s', '%s', '%s' or '%s' definition at top-level.",
                      KEYWORD_PACKAGE, KEYWORD_IMPORT, KEYWORD_ENUM, KEYWORD_TYPE, KEYWORD_COMPONENT);
            }
        }

        public void parseSchemaFile(@NotNull IElementType root) {
            PsiBuilder.Marker marker = builder.mark();
            while (builder.getTokenType() != null && !builder.eof()) {
                parseTopLevelDefinition();
            }
            marker.done(root);
        }
    }
}
