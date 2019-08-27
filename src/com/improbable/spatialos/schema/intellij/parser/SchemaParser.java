package com.improbable.spatialos.schema.intellij.parser;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class SchemaParser implements PsiParser {
    public static final SchemaParser SCHEMA_PARSER = new SchemaParser();

    public static final String KEYWORD_PACKAGE = "package";
    public static final String KEYWORD_IMPORT = "import";
    public static final String KEYWORD_ENUM = "enum";
    public static final String KEYWORD_TYPE = "type";
    public static final String KEYWORD_COMPONENT = "component";
    public static final String KEYWORD_ID = "id";
    public static final String KEYWORD_DATA = "data";
    public static final String KEYWORD_EVENT = "event";
    public static final String KEYWORD_COMMAND = "command";
    public static final String KEYWORD_ANNOTATION_START = "[";

    public static final IFileElementType SCHEMA_FILE = new IFileElementType(SchemaLanguage.SCHEMA_LANGUAGE);

    public static final IElementType KEYWORD = new Node("Keyword");
    public static final IElementType DEFINITION_NAME = new Node("Definition Name");

    public static final IElementType PACKAGE_DEFINITION = new Node("Package Definition");
    public static final IElementType PACKAGE_NAME = new Node("Package Name");

    public static final IElementType IMPORT_DEFINITION = new Node("Import Definition");
    public static final IElementType IMPORT_FILENAME = new Node("Import Filename");

    public static final IElementType TYPE_NAME = new Node("Type Name");
    public static final IElementType TYPE_PARAMETER_NAME = new Node("Type Parameter Name");

    public static final IElementType FIELD_TYPE = new PartNode("Field Type", SchemaAnnotator::highlightFieldType);
    public static final IElementType FIELD_NAME = new Node("Field Name");
    public static final IElementType FIELD_NUMBER = new Node("Field Number");
    public static final IElementType FIELD_REFERNCE = new Node("Field Reference");

    public static final IElementType ENUM_DEFINITION = new Node("Enum Definition");
    public static final IElementType ENUM_VALUE_DEFINITION = new PartNode("Enum Value Definition", SchemaAnnotator::highlightEnumEntry);

    public static final IElementType DATA_DEFINITION = new Node("Data Definition");
    public static final IElementType FIELD_DEFINITION = new PartNode("Field Definition", SchemaAnnotator::highlightField);
    public static final IElementType EVENT_DEFINITION = new Node("Event Definition");

    public static final IElementType TYPE_DEFINITION = new Node("Type Definition");
    public static final IElementType COMPONENT_DEFINITION = new Node("Component Definition");
    public static final IElementType COMPONENT_ID_DEFINITION = new Node("Component ID Definition");

    public static final IElementType COMMAND_DEFINITION = new PartNode("Command Definition", SchemaAnnotator::highlightCommand);

    public static final IElementType ANNOTATION_DEFINITION = new PartNode("Annotation Definition", SchemaAnnotator::highlightAnnotation);
    public static final IElementType ANNOTATION_QUALIFIER = new Node("Annotation Qualifier"); //TODO: rename
    public static final IElementType TYPE_NAME_REFERENCE = new Node("Type Name Reference");

    public static final IElementType FIELD_ARRAY = new Node("Annotation Field Array");

    public static final IElementType PRIMITIVE_INTEGER = new PartNode("Primitive Integer", SchemaAnnotator::highlightInteger);
    public static final IElementType PRIMITIVE_DOUBLE = new PartNode("Primitive Double", SchemaAnnotator::highlightDouble);
    public static final IElementType PRIMITIVE_BOOLEAN = new PartNode("Primitive Boolean", SchemaAnnotator::highlightBoolean);
    public static final IElementType PRIMITIVE_STRING = new PartNode("Primitive String", SchemaAnnotator::highlightString);

    public static final IElementType EMPTY_OPTION = new Node("Empty Option");

    public static final IElementType FIELD_LIST = new Node("Annotation Field List");

    public static final IElementType FIELD_MAP = new Node("Annotation Field Map");
    public static final IElementType FIELD_MAP_ENTRY = new Node("Field Map Entry");

    public static final IElementType FIELD_NEWINSTANCE = new PartNode("Annotation Field New Instance", SchemaAnnotator::highlightNewInstance);
    public static final IElementType FIELD_NEWINSTANCE_NAME = new Node("Annotation Field New Instance Name");

    public static final IElementType FIELD_ENUM_OR_INSTANCE = new PartNode("Annotation Field Enum Or Empty Instance", SchemaAnnotator::highlightEnumInstance);


    public static class Node extends IElementType {
        public Node(String debugName) {
            super(debugName, SchemaLanguage.SCHEMA_LANGUAGE);
        }
    }

    public static class PartNode extends Node {

        public final BiConsumer<PsiElement, AnnotationHolder> highlighter;

        public PartNode(String debugName, BiConsumer<PsiElement, AnnotationHolder> highlighter) {
            super(debugName);
            this.highlighter = highlighter;
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
            String filename = builder.getTokenText();
            consumeTokenAs(IMPORT_FILENAME);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, IMPORT_DEFINITION, Construct.STATEMENT,
                        "Expected ';' after '%s \"%s\"'.", KEYWORD_IMPORT, filename);
                return;
            }
            consumeTokenAs(null);
            marker.done(IMPORT_DEFINITION);
        }

        private @Nullable String parseTypeName(@NotNull PsiBuilder.Marker marker) {
            PsiBuilder.Marker typeMarker = builder.mark();
            String name = getIdentifier();
            String rawName = name;
            boolean isTransient = "transient".equals(rawName);

            if(isTransient) {
                consumeTokenAs(KEYWORD);
                typeMarker.drop();
                rawName = name = getIdentifier();
                typeMarker = builder.mark();
            }


            consumeTokenAs(TYPE_NAME);
            if (!isToken(SchemaLexer.LANGLE)) {
                if(isTransient) {
                    typeMarker.drop();
                    error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Cannot use transient on non collection", name);
                    return null;
                }
                typeMarker.done(FIELD_TYPE);
                return name;
            }
            name = name + '<';
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                typeMarker.drop();
                error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Expected typename after '%s'.", name);
                return null;
            }
            int numParams;
            switch (rawName) {
                case "map":
                    numParams = 2;
                    break;
                case "option":
                case "list":
                    numParams = 1;
                    break;
                default:
                    typeMarker.drop();
                    error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Unknown generic type '%s'.", rawName);
                    return null;
            }
            name = name + getIdentifier();
            consumeTokenAs(TYPE_PARAMETER_NAME);
            int params = 0;
            while (true) {
                params++;
                if (isToken(SchemaLexer.RANGLE)) {
                    name = name + '>';
                    consumeTokenAs(null);
                    if(params != numParams) {
                        typeMarker.drop();
                        error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Wrong number of generics. Expected %s, got %s", numParams, params);
                        return null;
                    }
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
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, FIELD_DEFINITION, Construct.STATEMENT, "Expected field name after '%s'.", typeName);
                return;
            }
            String fieldName = getIdentifier();
            consumeTokenAs(FIELD_NAME);
            if (!isToken(SchemaLexer.EQUALS)) {
                error(marker, FIELD_DEFINITION, Construct.STATEMENT,
                        "Expected '=' after '%s %s'.", typeName, fieldName);
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

        private void parseDataDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, DATA_DEFINITION, Construct.STATEMENT, "Expected typename after '%s'.", KEYWORD_DATA);
                return;
            }
            String typeName = parseTypeName(marker);
            if (typeName == null) {
                return;
            }
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, DATA_DEFINITION, Construct.STATEMENT,
                        "Expected ';' after '%s %s'.", KEYWORD_DATA, typeName);
                return;
            }
            consumeTokenAs(null);
            marker.done(DATA_DEFINITION);
        }

        private void parseEventDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, EVENT_DEFINITION, Construct.STATEMENT, "Expected typename after '%s'.", KEYWORD_EVENT);
                return;
            }
            String typeName = parseTypeName(marker);
            if (typeName == null) {
                return;
            }
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, EVENT_DEFINITION, Construct.STATEMENT,
                        "Expected field name after '%s %s'.", KEYWORD_EVENT, typeName);
                return;
            }
            String fieldName = getIdentifier();
            consumeTokenAs(FIELD_NAME);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, EVENT_DEFINITION, Construct.STATEMENT,
                        "Expected ';' after '%s %s %s'.", KEYWORD_EVENT, typeName, fieldName);
                return;
            }
            consumeTokenAs(null);
            marker.done(EVENT_DEFINITION);
        }

        private void parseComponentContents() {
            while (true) {
                if (isIdentifier(KEYWORD_ID)) {
                    parseComponentIdDefinition();
                    continue;
                }
                if (isIdentifier(KEYWORD_DATA)) {
                    parseDataDefinition();
                    continue;
                }
                if (isIdentifier(KEYWORD_EVENT)) {
                    parseEventDefinition();
                    continue;
                }
                if(isIdentifier(KEYWORD_COMMAND)) {
                    parseCommandDefinition();
                    continue;
                }
                if (isToken(SchemaLexer.IDENTIFIER)) {
                    parseFieldDefinition();
                    continue;
                }
                return;
            }
        }

        private void parseAnnotation() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(null);

            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, ANNOTATION_DEFINITION, Construct.STATEMENT, "Expected type after '['.");
                return;
            }
            consumeTokenAs(TYPE_NAME_REFERENCE);

            if(isToken(SchemaLexer.LPARENTHESES)) { //If the annotation has fields
                if(builder.lookAhead(2) == SchemaLexer.EQUALS) { //fully-qualified names
                    consumeTokenAs(null);
                    while(true) {
                        PsiBuilder.Marker element = builder.mark();
                        if (!isToken(SchemaLexer.IDENTIFIER)) {
                            element.drop();
                            error(marker, ANNOTATION_DEFINITION, Construct.STATEMENT, "Expected field identifier");
                            return;
                        }
                        consumeTokenAs(FIELD_REFERNCE);
                        if (!isToken(SchemaLexer.EQUALS)) {
                            element.drop();
                            error(marker, ANNOTATION_DEFINITION, Construct.STATEMENT, "Expected '='");
                            return;
                        }
                        consumeTokenAs(null);
                        parseAnnotationField();

                        element.done(ANNOTATION_QUALIFIER);

                        if(isToken(SchemaLexer.RPARENTHESES)) {
                            consumeTokenAs(null);
                            break;
                        }

                        if(!isToken(SchemaLexer.COMMA)) {
                            error(marker, ANNOTATION_DEFINITION, Construct.STATEMENT, "Expected ',' or end of annotation");
                            return;
                        }
                        consumeTokenAs(null);
                    }
                } else {
                    parseAnnotationFieldArray();
                }
            }

            if(!isToken(SchemaLexer.RBRACKET)) {
                error(marker, ANNOTATION_DEFINITION, Construct.STATEMENT, "Expected end of annotation ']'");
                return;
            }
            consumeTokenAs(null);

            marker.done(ANNOTATION_DEFINITION);
        }

        private void parseAnnotationFieldArray() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(null);
            if(!isToken(SchemaLexer.RPARENTHESES)) {
                while (true) {
                    if(builder.getTokenText() == null) { //Something gone wrong. Invalid input?
                        break;
                    }
                    parseAnnotationField();
                    if(isToken(SchemaLexer.RPARENTHESES)) {
                        break;
                    }
                    if(!isToken(SchemaLexer.COMMA)) {
                        error(marker, FIELD_ARRAY, Construct.STATEMENT, "Expected ',' or end of array, found %s", builder.getTokenText());
                        return;
                    }
                    consumeTokenAs(null);
                }
            }
            consumeTokenAs(null);
            marker.done(FIELD_ARRAY);
        }

        private void parseAnnotationField() {
            if(isToken(SchemaLexer.INTEGER)) {
                PsiBuilder.Marker marker = builder.mark();
                String text = builder.getTokenText();
                consumeTokenAs(null);
                if(isIdentifier(".")) {
                    consumeTokenAs(null);
                    if(!isToken(SchemaLexer.INTEGER)) {
                        error(marker, PRIMITIVE_DOUBLE, Construct.STATEMENT, "Expected numbers after %s%s", text, builder.getTokenText());
                        return;
                    }
                    consumeTokenAs(null);
                    marker.done(PRIMITIVE_DOUBLE);
                    return;
                }
                marker.done(PRIMITIVE_INTEGER);
                return;
            }
            if(isToken(SchemaLexer.BOOLEAN)) {
                consumeTokenAs(PRIMITIVE_BOOLEAN);
                return;
            }
            if(isToken(SchemaLexer.STRING)) {
                consumeTokenAs(PRIMITIVE_STRING);
                return;
            }
            if(isToken(SchemaLexer.LBRACKET)) { //List
                PsiBuilder.Marker marker = builder.mark();
                consumeTokenAs(null);
                if(isToken(SchemaLexer.RBRACKET)) { //Empty list
                    consumeTokenAs(null);
                } else {
                    while(true) {
                        parseAnnotationField();

                        if(isToken(SchemaLexer.RBRACKET)) {
                            consumeTokenAs(null);
                            break;
                        }
                        if(!isToken(SchemaLexer.COMMA)) {
                            error(marker, FIELD_LIST, Construct.STATEMENT, "Expected ',' or end of array");
                            return;
                        }
                        consumeTokenAs(null);
                    }
                }
                marker.done(FIELD_LIST);
                return;
            }

            if(isToken(SchemaLexer.LBRACE)) { //Map
                PsiBuilder.Marker marker = builder.mark();
                consumeTokenAs(null);
                if(isToken(SchemaLexer.RBRACE)) { //Empty map
                    consumeTokenAs(null);
                } else {
                    while(true) {
                        PsiBuilder.Marker entryMarker = builder.mark();
                        parseAnnotationField();
                        if(!isToken(SchemaLexer.COLON)) {
                            entryMarker.drop();
                            error(marker, FIELD_MAP, Construct.STATEMENT, "Expected ':' in map");
                            return;
                        }
                        consumeTokenAs(null); // ':'
                        parseAnnotationField();
                        entryMarker.done(FIELD_MAP_ENTRY);

                        if(isToken(SchemaLexer.RBRACE)) {
                            consumeTokenAs(null);
                            break;
                        }
                        if(!isToken(SchemaLexer.COMMA)) {
                            error(marker, FIELD_MAP, Construct.STATEMENT, "Expected ',' or end of map");
                            return;
                        }
                        consumeTokenAs(null);
                    }
                }
                marker.done(FIELD_MAP);
                return;
            }
            if(isIdentifier("_")) {//empty
                consumeTokenAs(EMPTY_OPTION);
                return;
            }
            if(isToken(SchemaLexer.IDENTIFIER)) {
                if(builder.lookAhead(1) == SchemaLexer.LPARENTHESES) { //Initiate a new object
                    PsiBuilder.Marker marker = builder.mark();
                    consumeTokenAs(FIELD_NEWINSTANCE_NAME);
                    parseAnnotationFieldArray();
                    marker.done(FIELD_NEWINSTANCE);
                    return;
                } else { //Enum or empty instance value
                    consumeTokenAs(FIELD_ENUM_OR_INSTANCE);
                    return;
                }
            }
            error(null, null, Construct.STATEMENT, "Unknown Type %s", builder.getTokenText());
        }


        private void parseCommandDefinition() {
            PsiBuilder.Marker marker = builder.mark();
            consumeTokenAs(KEYWORD);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, COMMAND_DEFINITION, Construct.STATEMENT, "Expected command response after 'command'.");
                return;
            }
            String response = getIdentifier();
            consumeTokenAs(TYPE_NAME_REFERENCE);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, COMMAND_DEFINITION, Construct.STATEMENT,
                        "Expected command name after 'command %s'.", response);
                return;
            }
            String name = getIdentifier();
            consumeTokenAs(FIELD_NAME);
            if (!isToken(SchemaLexer.LPARENTHESES)) {
                error(marker, COMMAND_DEFINITION, Construct.STATEMENT,
                        "Expected '(' after 'command %s %s'.", response, name);
                return;
            }
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.IDENTIFIER)) {
                error(marker, COMMAND_DEFINITION, Construct.STATEMENT,
                        "Expected command request after 'command %s %s('.", response, name);
                return;
            }
            String request = getIdentifier();
            consumeTokenAs(TYPE_NAME_REFERENCE);
            if (!isToken(SchemaLexer.RPARENTHESES)) {
                error(marker, COMMAND_DEFINITION, Construct.STATEMENT,
                        "Expected ')' after 'command %s %s(%s'.", response, name, request);
                return;
            }
            consumeTokenAs(null);
            if (!isToken(SchemaLexer.SEMICOLON)) {
                error(marker, COMMAND_DEFINITION, Construct.STATEMENT,
                        "Expected ';' after 'command %s %s(%s)'.", response, name, request);
                return;
            }
            consumeTokenAs(null);
            marker.done(COMMAND_DEFINITION);
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
            } else if(builder.getTokenText() != null && builder.getTokenText().equals(KEYWORD_ANNOTATION_START)) {
                parseAnnotation();
            } else {
                error(null, null, Construct.TOP_LEVEL,
                        "Expected '%s', '%s', '%s', '%s' or '%s' definition at top-level.",
                        KEYWORD_PACKAGE, KEYWORD_IMPORT, KEYWORD_ENUM, KEYWORD_TYPE, KEYWORD_COMPONENT);
            }
        }

        public void parseSchemaFile(@NotNull IElementType root) {
            PsiBuilder.Marker outerMarker = builder.mark();
            while (builder.getTokenType() != null && !builder.eof()) {
                parseTopLevelDefinition();
            }
            outerMarker.done(root);
        }
    }
}