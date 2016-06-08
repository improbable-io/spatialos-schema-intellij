package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemaParser implements PsiParser {
    public static final SchemaParser SCHEMA_PARSER = new SchemaParser();

    public static final String KEYWORD_PACKAGE = "package";
    public static final String KEYWORD_IMPORT = "import";
    public static final String KEYWORD_ENUM = "enum";
    public static final String KEYWORD_TYPE = "type";
    public static final String KEYWORD_COMPONENT = "component";
    public static final String KEYWORD_OPTION = "option";
    public static final String KEYWORD_ID = "id";

    public static final IFileElementType SCHEMA_FILE = new IFileElementType(SchemaLanguage.SCHEMA_LANGUAGE);

    public static final IElementType KEYWORD = new Node("Keyword");
    public static final IElementType DEFINITION_NAME = new Node("Definition Name");

    public static final IElementType PACKAGE_DEFINITION = new Node("Package Definition");
    public static final IElementType PACKAGE_NAME = new Node("Package Name");

    public static final IElementType IMPORT_DEFINITION = new Node("Import Definition");
    public static final IElementType IMPORT_FILENAME = new Node("Import Filename");

    public static final IElementType OPTION_DEFINITION = new Node("Option Definition");
    public static final IElementType OPTION_NAME = new Node("Option Name");
    public static final IElementType OPTION_VALUE = new Node("Option Value");

    public static final IElementType TYPE_NAME = new Node("Type Name");
    public static final IElementType TYPE_PARAMETER_NAME = new Node("Type Parameter Name");

    public static final IElementType FIELD_TYPE = new Node("Field Type");
    public static final IElementType FIELD_NAME = new Node("Field Name");
    public static final IElementType FIELD_NUMBER = new Node("Field Number");

    public static final IElementType ENUM_DEFINITION = new Node("Enum Definition");
    public static final IElementType ENUM_VALUE_DEFINITION = new Node("Enum Value Definition");

    public static final IElementType TYPE_DEFINITION = new Node("Type Definition");
    public static final IElementType FIELD_DEFINITION = new Node("Field Definition");

    public static final IElementType COMPONENT_DEFINITION = new Node("Component Definition");
    public static final IElementType COMPONENT_ID_DEFINITION = new Node("Component ID Definition");

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

        private void error(@Nullable PsiBuilder.Marker marker, IElementType elementType, Construct construct,
                           String s, Object... args) {
            if (marker != null) {
                marker.done(elementType);
            }
            String errorMessage = String.format(s, args);
            PsiBuilder.Marker errorMarker = builder.mark();

            while (builder.getTokenType() != null && !builder.eof()) {
                if ((construct == Construct.STATEMENT || construct == Construct.TOP_LEVEL) &&
                    isToken(SchemaLexer.SEMICOLON)) {
                    errorMarker.error(errorMessage);
                    builder.advanceLexer();
                    return;
                }
                if ((construct == Construct.BRACES || construct == Construct.TOP_LEVEL) &&
                    isToken(SchemaLexer.RBRACE)) {
                    errorMarker.error(errorMessage);
                    builder.advanceLexer();
                    return;
                }
                if (construct == Construct.STATEMENT && isToken(SchemaLexer.RBRACE)) {
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

        private boolean isToken(IElementType token) {
            return builder.getTokenType() == token;
        }

        private boolean isIdentifier(@NotNull String identifier) {
            return builder.getTokenType() == SchemaLexer.IDENTIFIER &&
                    builder.getTokenText() != null && builder.getTokenText().equals(identifier);
        }

        private void consumeTokenAs(@Nullable IElementType nodeType) {
            PsiBuilder.Marker marker = nodeType == null ? null : builder.mark();
            builder.advanceLexer();
            if (marker != null) {
                marker.done(nodeType);
            }
        }

        private void parsePackageDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, PACKAGE_DEFINITION, Construct.STATEMENT,
                      "Expected a package name after '%s'.", KEYWORD_PACKAGE);
                return;
            }
            consumeTokenAs(PACKAGE_NAME);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, PACKAGE_DEFINITION, Construct.STATEMENT,
                      "Expected ';' after %s definition.", KEYWORD_PACKAGE);
                return;
            }
            consumeTokenAs(null);
            marker.done(PACKAGE_DEFINITION);
        }

        private void parseImportDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.STRING)) {
                error(marker, IMPORT_DEFINITION, Construct.STATEMENT,
                      "Expected a quoted filename after '%s'.", KEYWORD_IMPORT);
                return;
            }
            String filename = getString();
            consumeTokenAs(IMPORT_FILENAME);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, IMPORT_DEFINITION, Construct.STATEMENT,
                      "Expected ';' after '%s \"%s\"'.", KEYWORD_IMPORT, filename);
                return;
            }
            consumeTokenAs(null);
            marker.done(IMPORT_DEFINITION);
        }

        private void parseOptionDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, OPTION_DEFINITION, Construct.STATEMENT,
                      "Expected identifier after '%s'.", KEYWORD_OPTION);
                return;
            }
            String name = getIdentifier();
            consumeTokenAs(OPTION_NAME);
            if (!isToken(SchemaLexer.EQUALS)) {
                error(marker, OPTION_DEFINITION, Construct.STATEMENT,
                      "Expected '=' after '%s %s'.", KEYWORD_OPTION, name);
                return;
            }
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, OPTION_DEFINITION, Construct.STATEMENT,
                      "Expected option value after '%s %s = '.", KEYWORD_OPTION, name);
                return;
            }
            String value = getIdentifier();
            consumeTokenAs(OPTION_VALUE);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, OPTION_DEFINITION, Construct.STATEMENT,
                      "Expected ';' after '%s %s = %s'.", KEYWORD_OPTION, name, value);
                return;
            }
            consumeTokenAs(null);
            marker.done(OPTION_DEFINITION);
        }

        private @Nullable String parseTypeName(@NotNull PsiBuilder.Marker marker) {
            PsiBuilder.Marker typeMarker = builder.mark();
            String name = getIdentifier();
            consumeTokenAs(TYPE_NAME);
            if (!isToken(SchemaLexer.LANGLE)) {
                typeMarker.done(FIELD_TYPE);
                return name;
            }
            name = name + '<';
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                typeMarker.drop();
                error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Expected typename after '%s<'.", name);
                return null;
            }
            name = name + getIdentifier();
            consumeTokenAs(TYPE_PARAMETER_NAME);
            while (true) {
                if (isToken(SchemaLexer.RANGLE)) {
                    name = name + '>';
                    consumeTokenAs(null);
                    typeMarker.done(FIELD_TYPE);
                    return name;
                }
                if (isToken(SchemaLexer.COMMA)) {
                    name = name + ", ";
                    consumeTokenAs(null);
                    if (!isToken(SchemaLexer.IDENTIFIER)) {
                        typeMarker.drop();
                        error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Expected typename after ','.");
                        return null;
                    }
                    name = name + getIdentifier();
                    consumeTokenAs(TYPE_PARAMETER_NAME);
                    continue;
                }
                typeMarker.drop();
                error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Invalid '%s' inside <>.", getTokenText());
                return null;
            }
        }

        private void parseFieldDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            String typeName = parseTypeName(marker);
            if (typeName == null) {
                return;
            }
            if (isToken(SchemaLexer.SEMICOLON)) {
                consumeTokenAs(null);
                marker.done(FIELD_DEFINITION);
                return;
            }
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, FIELD_DEFINITION, Construct.STATEMENT,
                      "Expected ';' or field name after '%s'.", typeName);
                return;
            }
            String fieldName = getIdentifier();
            consumeTokenAs(FIELD_NAME);
            if (isToken(SchemaLexer.SEMICOLON)) {
                consumeTokenAs(null);
                return;
            }
            if (!isToken(SchemaLexer.EQUALS)) {
                error(marker, FIELD_DEFINITION, Construct.STATEMENT,
                      "Expected ';' or '=' after '%s %s'.", typeName, fieldName);
                return;
            }
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.INTEGER)) {
                error(marker, FIELD_DEFINITION, Construct.STATEMENT,
                      "Expected field number after '%s %s = '.", typeName, fieldName);
                return;
            }
            int fieldNumber = getInteger();
            consumeTokenAs(FIELD_NUMBER);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, FIELD_DEFINITION, Construct.STATEMENT,
                      "Expected ';' after '%s %s = %d'.", typeName, fieldName, fieldNumber);
                return;
            }
            consumeTokenAs(null);
            marker.done(FIELD_DEFINITION);
        }

        private void parseEnumContents() {
            while (isToken(SchemaLexer.IDENTIFIER)) {
                PsiBuilder.Marker marker = builder.mark();
                String name = getIdentifier();
                consumeTokenAs(FIELD_NAME);
                if (!isToken(SchemaLexer.EQUALS)) {
                    error(marker, ENUM_VALUE_DEFINITION, Construct.STATEMENT, "Expected '=' after '%s'.", name);
                    continue;
                }
                consumeTokenAs(null);
                if (!isToken(SchemaLexer.INTEGER)) {
                    error(marker, ENUM_VALUE_DEFINITION, Construct.STATEMENT,
                          "Expected integer enum value after '%s = '.", name);
                    continue;
                }
                int value = getInteger();
                consumeTokenAs(FIELD_NUMBER);
                if (!isToken(SchemaLexer.SEMICOLON)) {
                    error(marker, ENUM_VALUE_DEFINITION, Construct.STATEMENT,
                          "Expected ';' after '%s = %d'.", name, value);
                    continue;
                }
                consumeTokenAs(null);
                marker.done(ENUM_VALUE_DEFINITION);
            }
        }

        private void parseTypeContents() {
            while (true) {
                if (isIdentifier(KEYWORD_OPTION)) {
                    PsiBuilder.Marker marker = builder.mark();
                    builder.advanceLexer();
                    boolean lookaheadIsOption = !isToken(SchemaLexer.LANGLE);
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
                if (isToken(SchemaLexer.IDENTIFIER)) {
                    parseFieldDefinition();
                    continue;
                }
                return;
            }
        }

        private void parseComponentIdDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.EQUALS)) {
                error(marker, COMPONENT_ID_DEFINITION, Construct.STATEMENT,
                      "Expected '=' after '%s'.", KEYWORD_ID);
                return;
            }
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.INTEGER)) {
                error(marker, COMPONENT_ID_DEFINITION, Construct.STATEMENT,
                      "Expected integer ID value after '%s = '.", KEYWORD_ID);
                return;
            }
            int value = getInteger();
            consumeTokenAs(FIELD_NUMBER);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, COMPONENT_ID_DEFINITION, Construct.STATEMENT,
                      "Expected ';' after '%s = %d'.", KEYWORD_ID, value);
                return;
            }
            consumeTokenAs(null);
            marker.done(COMPONENT_ID_DEFINITION);
        }

        private void parseComponentContents() {
            while (true) {
                if (isIdentifier(KEYWORD_OPTION)) {
                    PsiBuilder.Marker marker = builder.mark();
                    builder.advanceLexer();
                    boolean lookaheadIsOption = !isToken(SchemaLexer.LANGLE);
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
                if (isToken(SchemaLexer.IDENTIFIER)) {
                    parseFieldDefinition();
                    continue;
                }
                return;
            }
        }

        private void parseEnumDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, ENUM_DEFINITION, Construct.BRACES, "Expected identifier after '%s'.", KEYWORD_ENUM);
                return;
            }
            String name = getIdentifier();
            consumeTokenAs(DEFINITION_NAME);
            if (!isToken(SchemaLexer.LBRACE)) {
                error(marker, ENUM_DEFINITION, Construct.BRACES, "Expected '{' after '%s %s'.", KEYWORD_ENUM, name);
                return;
            }
            consumeTokenAs(null);
            parseEnumContents();
            if (!isToken(SchemaLexer.RBRACE)) {
                error(marker, ENUM_DEFINITION, Construct.BRACES,
                      "Invalid '%s' inside %s %s.", getTokenText(), KEYWORD_ENUM, name);
                return;
            }
            consumeTokenAs(null);
            marker.done(ENUM_DEFINITION);
        }

        private void parseTypeDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, TYPE_DEFINITION, Construct.BRACES, "Expected identifier after '%s'.", KEYWORD_TYPE);
                return;
            }
            String name = getIdentifier();
            consumeTokenAs(DEFINITION_NAME);
            if (!isToken(SchemaLexer.LBRACE)) {
                error(marker, TYPE_DEFINITION, Construct.BRACES, "Expected '{' after '%s %s'.", KEYWORD_TYPE, name);
                return;
            }
            consumeTokenAs(null);
            parseTypeContents();
            if (!isToken(SchemaLexer.RBRACE)) {
                error(marker, TYPE_DEFINITION, Construct.BRACES,
                      "Invalid '%s' inside %s %s.", getTokenText(), KEYWORD_TYPE, name);
                return;
            }
            consumeTokenAs(null);
            marker.done(TYPE_DEFINITION);
        }

        private void parseComponentDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, COMPONENT_DEFINITION, Construct.BRACES,
                      "Expected identifier after '%s'.", KEYWORD_COMPONENT);
                return;
            }
            String name = getIdentifier();
            consumeTokenAs(DEFINITION_NAME);
            if (!isToken(SchemaLexer.LBRACE)) {
                error(marker, COMPONENT_DEFINITION, Construct.BRACES,
                      "Expected '{' after '%s %s'.", KEYWORD_COMPONENT, name);
                return;
            }
            consumeTokenAs(null);
            parseComponentContents();
            if (!isToken(SchemaLexer.RBRACE)) {
                error(marker, COMPONENT_DEFINITION, Construct.BRACES,
                      "Invalid '%s' inside %s %s.", getTokenText(), KEYWORD_COMPONENT, name);
                return;
            }
            consumeTokenAs(null);
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
                error(null, null, Construct.TOP_LEVEL,
                      "Expected '%s', '%s', '%s', '%s' or '%s' definition at top-level.",
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
