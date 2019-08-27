package com.improbable.spatialos.schema.intellij.parser;

import com.google.common.collect.Sets;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.twelvemonkeys.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class SchemaAnnotator implements Annotator {
    public static final List<String> OPTION_VALUES = Arrays.asList("true", "false");
    private static final List<String> BUILT_IN_GENERIC_TYPES = Arrays.asList("option", "list", "map");
    private static final List<String> BUILT_IN_TYPES = Arrays.asList(
            "double", "float", "string", "bytes", "int32", "int64", "uint32", "uint64", "sint32", "sint64", "fixed32",
            "fixed64", "sfixed32", "sfixed64", "bool"
    );

    private static final List<String> STANDARD_LIBRARY = Arrays.asList(
            "Position", "Coordinates", "WorkerAttributeSet", "WorkerRequirementSet", "EntityAcl", "Persistence", "Metadata", "Interest"
    );

    public static final Pattern BOOL = Pattern.compile("(?i)(?:true|false)");
    public static final Pattern INTEGER = Pattern.compile("\\d+");
    public static final Pattern DECIMAL = Pattern.compile("\\d+\\.\\d+");
    public static final Pattern STRING =  Pattern.compile("\".*?\"");

    public static final String BOOL_ELEMENT = "bool";
    public static final List<String> INTEGER_ELEMENTS = Arrays.asList("uint32", "uint64", "int32", "int64", "sint32", "sint64", "fixed32", "fixed64", "sfixed32", "sfixed64", "EntityId");
    public static final List<String> DECIMAL_ELEMENTS = Arrays.asList("float", "double");
    public static final List<String> STRING_ELEMENTS = Arrays.asList("string", "bytes");


    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element.getNode().getElementType() == SchemaParser.KEYWORD) {
            holder.createInfoAnnotation(element, null).setTextAttributes(DefaultLanguageHighlighterColors.KEYWORD);
        } else {
            if(element.getNode().getElementType() instanceof SchemaParser.PartNode) {
                ((SchemaParser.PartNode) element.getNode().getElementType()).highlighter.accept(element, holder); // eh. Maybe should do a else if ?
            }
        }
    }

    public static void highlightFieldType(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiElement[] children = element.getChildren();
        if(children.length > 0) {
            checkFieldTypeValid(children[0], holder);
            if(BUILT_IN_GENERIC_TYPES.contains(children[0].getText())) {
                for (int i = 1; i < children.length; i++) {
                    checkFieldTypeValid(children[i], holder);
                }
            }
        }
    }

    public static void checkFieldTypeValid(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if(BUILT_IN_TYPES.contains(element.getText()) || BUILT_IN_GENERIC_TYPES.contains(element.getText())) {
            holder.createInfoAnnotation(element, null).setTextAttributes(DefaultLanguageHighlighterColors.KEYWORD);
        } else {
            String text = element.getText();
            PsiElement ref = resolveElement(element, text);
            if(ref == null && !isImprobableElement(element, text)) {
                holder.createErrorAnnotation(element, "Unable to find type " + element.getText());
            }
        }
    }

    public static void highlightField(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiElement[] children = element.getChildren();
        if(children.length == 3) {
            holder.createInfoAnnotation(children[1], null).setTextAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD);
            holder.createInfoAnnotation(children[2], null).setTextAttributes(DefaultLanguageHighlighterColors.NUMBER);
        }
    }

    public static void highlightEnumEntry(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiElement[] children = element.getChildren();
        if(children.length == 2) {
            holder.createInfoAnnotation(children[0], null).setTextAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD);
            holder.createInfoAnnotation(children[1], null).setTextAttributes(DefaultLanguageHighlighterColors.NUMBER);
        }    }

    public static void highlightCommand(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiElement[] children = element.getChildren();
        if(children.length == 4) {
            holder.createInfoAnnotation(children[2], null).setTextAttributes(DefaultLanguageHighlighterColors.INSTANCE_METHOD);
        }
    }

    public static void highlightAnnotation(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiElement[] children = element.getChildren();
        if(children.length > 0) {
            holder.createInfoAnnotation(children[0], null).setTextAttributes(DefaultLanguageHighlighterColors.CLASS_REFERENCE);
            if(children.length > 1) {
                if(children[1].getNode().getElementType() == SchemaParser.ANNOTATION_QUALIFIER) { //Qualified Names
                    PsiElement ref = resolveElement(element, children[0].getText());
                    if(ref != null) {
                        for (int i = 1; i < children.length; i++) {
                            PsiElement[] entryChildren = children[i].getChildren();
                            if(entryChildren.length > 1) {
                                if(entryChildren[1].getNode().getElementType() instanceof SchemaParser.PartNode) {
                                    ((SchemaParser.PartNode) entryChildren[1].getNode().getElementType()).highlighter.accept(entryChildren[1], holder);
                                }
                            }
                            List<PsiElement> orderedFields = getOrderedFields(ref);
                            if(orderedFields.size() != children.length - 1) {
                                holder.createErrorAnnotation(element, "Invalid parameter count. Expected " + orderedFields.size() + " found " + (children.length - 1)).setTextAttributes(HighlighterColors.BAD_CHARACTER);
                            } else {
                                PsiElement[] field = orderedFields.get(i - 1).getChildren();
                                if(!entryChildren[0].getText().equals(field[1].getText())) {
                                    holder.createErrorAnnotation(entryChildren[0], "Invalid field name '" + entryChildren[0].getText() + "' found. Did you mean '" + field[1].getText() + "'?");
                                }
                                checkFieldValidity(holder, entryChildren[1], field[0]);
                            }
                        }
                    } else {
                        holder.createErrorAnnotation(element, "Unable to find type " + children[0].getText()).setTextAttributes(HighlighterColors.BAD_CHARACTER);
                    }
                } else {
                    highlightTypeArray(element, holder, 2);
                }
            }
        }
    }

    public static void highlightInteger(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        holder.createInfoAnnotation(element, null).setTextAttributes(DefaultLanguageHighlighterColors.NUMBER);
    }

    public static void highlightDouble(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        holder.createInfoAnnotation(element, null).setTextAttributes(DefaultLanguageHighlighterColors.NUMBER);
    }

    public static void highlightBoolean(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        holder.createInfoAnnotation(element, null).setTextAttributes(DefaultLanguageHighlighterColors.KEYWORD);
    }

    public static void highlightString(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        holder.createInfoAnnotation(element, null).setTextAttributes(DefaultLanguageHighlighterColors.STRING);
    }

    public static void highlightNewInstance(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        highlightTypeArray(element, holder, 2);
        if(element.getChildren().length > 0) {
            int off = element.getTextOffset();
            for (String clazz : element.getChildren()[0].getText().split("\\.")) {
                holder.createInfoAnnotation(new TextRange(off, off + clazz.length()), null).setTextAttributes(DefaultLanguageHighlighterColors.CLASS_NAME);
                off += clazz.length() + 1;
            }
        }
    }

    public static void highlightTypeArray(@NotNull PsiElement element, @NotNull AnnotationHolder holder, int padding) {
        PsiElement[] children = element.getChildren();
        if(children.length > 0) {
            PsiElement ref = resolveElement(element, children[0].getText());
            if(ref == null && !isImprobableElement(element, children[0].getText())) {
                holder.createErrorAnnotation(element, "Unable to find type " + children[0].getText()).setTextAttributes(HighlighterColors.BAD_CHARACTER);
            } else {
                if(children.length > 1) {
                    //The 2 here is used as the type definition node can have other nodes in its children, before the actual values
                    List<PsiElement> orderedFields = getOrderedFields(ref);
                    if(orderedFields.size() != children[1].getChildren().length) {
                        holder.createErrorAnnotation(element, "Invalid parameter count. Expected " + orderedFields.size() + " found " + children[1].getChildren().length).setTextAttributes(HighlighterColors.BAD_CHARACTER);
                    } else {
                        for (int i = 0; i < children[1].getChildren().length; i++) {
                            checkFieldValidity(holder, children[1].getChildren()[i], orderedFields.get(i).getChildren()[0]);
                        }
                    }
                }
            }
        }
    }

    public static void checkFieldValidity(@NotNull AnnotationHolder holder, PsiElement fieldElement, PsiElement actualField) {

        String type = actualField.getText();
        String typed = fieldElement.getText();

        if(BOOL_ELEMENT.equals(type)) {
            if(!BOOL.matcher(typed).matches()) {
                holder.createErrorAnnotation(fieldElement, "Expected true or false. Found '" + typed + "'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
            }
        } else if(INTEGER_ELEMENTS.contains(type)) {
            if(!INTEGER.matcher(typed).matches()) {
                holder.createErrorAnnotation(fieldElement, "Expected an integer. Found '" + typed + "'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
            }
        } else if(DECIMAL_ELEMENTS.contains(type)) {
            if(!DECIMAL.matcher(typed).matches()) {
                holder.createErrorAnnotation(fieldElement, "Expected a decimal. Found '" + typed + "'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
            }
        } else if(STRING_ELEMENTS.contains(type)) {
            if(!STRING.matcher(typed).matches()) {
                holder.createErrorAnnotation(fieldElement, "Expected a string. Found '" + typed + "'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
            }
        } else { //Not a primitive
            String actual = type;
            List<PsiElement> generics = new LinkedList<>();
            if(actualField.getChildren().length > 1) { //Generics
                actual = actualField.getChildren()[0].getText();
                for (int gen = 1; gen < actualField.getChildren().length; gen++) {
                    generics.add(actualField.getChildren()[gen]);
                }
            }

            if(typed.equals("_")) {
                if(!actual.equals("option")) {
                    holder.createErrorAnnotation(fieldElement, "Illegal Character. Empty options are not allowed on " + type + "s");
                }
            } else {
                if(actual.equals("list")) {
                    if(typed.trim().startsWith("[") && typed.trim().endsWith("]")) {
                        for (PsiElement field : fieldElement.getChildren()) {
                            checkFieldValidity(holder, field, generics.get(0));
                        }
                    } else {
                        if(!typed.trim().startsWith("[")) {
                            holder.createErrorAnnotation(fieldElement, "Lists should start with '['").setTextAttributes(HighlighterColors.BAD_CHARACTER);
                        } else if(!typed.trim().endsWith("]")) {
                            holder.createErrorAnnotation(fieldElement, "Lists should end with ']'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
                        }
                    }
                } else if(actual.equals("option")) {
                    checkFieldValidity(holder, fieldElement, generics.get(0));
                } else if(actual.equals("map")) {
                    if(typed.trim().startsWith("{") && typed.trim().endsWith("}")) {
                        for (PsiElement field : fieldElement.getChildren()) {
                            if(field.getChildren().length > 1) {
                                checkFieldValidity(holder, field.getChildren()[0], generics.get(0));
                                checkFieldValidity(holder, field.getChildren()[1], generics.get(1));
                            } else {
                                holder.createErrorAnnotation(field, "Invalid map entry. Format should be " + generics.get(0).getText() + ":" + generics.get(1).getText()).setTextAttributes(HighlighterColors.BAD_CHARACTER);
                            }
                        }
                    } else {
                        if(!typed.trim().startsWith("{")) {
                            holder.createErrorAnnotation(fieldElement, "Maps should start with '{'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
                        } else if(!typed.trim().endsWith("}")) {
                            holder.createErrorAnnotation(fieldElement, "Maps should end with '}'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
                        }
                    }
                } else { //An enum or new object.
                    PsiElement element = resolveElement(actualField, actual);
                    if(element != null) {
                        if(element.getNode().getElementType() == SchemaParser.ENUM_DEFINITION) { //Enum
                            PsiElement typedElement = resolveElement(fieldElement, typed);
                            if(typedElement == null || typedElement.getNode().getElementType() != SchemaParser.ENUM_VALUE_DEFINITION) {
                                holder.createErrorAnnotation(fieldElement, "Invalid type " + typed + ". Expected enum-type: '" + actual + "'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
                            } else {
                                //Enum definitions start with keyword, name
                                boolean valid = false;
                                for (int i = 2; i < element.getChildren().length; i++) {
                                    if (element.getChildren()[i] == typedElement) {
                                        valid = true;
                                    }
                                }
                                if(!valid) {
                                    holder.createErrorAnnotation(fieldElement, "Enum '" + typed + "' is not of type '" + actual + "'").setTextAttributes(HighlighterColors.BAD_CHARACTER);
                                }
                            }
                        } else { //New instance
                            PsiElement[] children = fieldElement.getChildren();
                            if(children.length > 0) {
                                if(resolveElement(fieldElement, children[0].getText()) != resolveElement(actualField, type)) {
                                    holder.createErrorAnnotation(fieldElement, "Expected '" + actual + "' found '" + children[0].getText() + "'");
                                }
                            } else {
                                holder.createErrorAnnotation(fieldElement, "Expected '" + actual + "' found '" + typed + "'");
                            }
                        }
                    }
                }
            }
        }
    }

    public static void highlightEnumInstance(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiElement ref = resolveElement(element, element.getText());
        if(ref == null) {
            holder.createErrorAnnotation(element, "Unable to find type/enum " + element.getText()).setTextAttributes(HighlighterColors.BAD_CHARACTER);
        } else if(ref.getNode().getElementType() == SchemaParser.TYPE_DEFINITION)  {
            int off = element.getTextOffset();
            for (String clazz : element.getText().split("\\.")) {
                holder.createInfoAnnotation(new TextRange(off, off + clazz.length()), null).setTextAttributes(DefaultLanguageHighlighterColors.CLASS_NAME);
                off += clazz.length() + 1;
            }
        } else if(ref.getNode().getElementType() == SchemaParser.ENUM_VALUE_DEFINITION) {
            String[] names = element.getText().split("\\.");
            int off = element.getTextOffset();
            for (int i = 0; i < names.length; i++) {
                String clazz = names[i];
                holder.createInfoAnnotation(new TextRange(off, off + clazz.length()), null).setTextAttributes(i == names.length - 1 ? DefaultLanguageHighlighterColors.STATIC_FIELD : DefaultLanguageHighlighterColors.CLASS_NAME);
                off += clazz.length() + 1;
            }
        }
    }


    public static boolean isImprobableElement(PsiElement element, String text) {
        return text.equals("EntityId") || (hasStandardLibraryImport(element) && text.startsWith("improbable.") && STANDARD_LIBRARY.contains(text.substring(11))); //substring 11 is to get the element name
    }

    public static boolean hasStandardLibraryImport(PsiElement element) {
        return Arrays.stream(element.getContainingFile().getChildren())
                .anyMatch(child ->
                        child.getNode().getElementType() == SchemaParser.IMPORT_DEFINITION &&
                                child.getChildren()[1].getText().equals("\"improbable/standard_library.schema\"")
                );
    }


    public static PsiElement resolveElement(@NotNull PsiElement element, String elementName) {
        return resolveElement(element, elementName, true);
    }

    public static PsiElement resolveElement(@NotNull PsiElement element, String elementName, boolean searchImports) {
        PsiElement out = null;
        String[] splitNames = elementName.split("\\.");
        PsiElement ref = element.getContainingFile();

        for (int i = 0; i < splitNames.length; i++) {
            if(i == splitNames.length - 2) { //Second last. This is where i can search for enums
                for (PsiElement child : ref.getChildren()) {
                    if(child.getNode().getElementType() == SchemaParser.ENUM_DEFINITION) {
                        PsiElement[] grandChildren = child.getChildren();
                        if(grandChildren.length > 1 && grandChildren[1].getText().equals(splitNames[i])) {
                            for (PsiElement grandChild : grandChildren) {
                                if(grandChild.getNode().getElementType() == SchemaParser.ENUM_VALUE_DEFINITION) {
                                    PsiElement[] greatGrandChild = grandChild.getChildren();
                                    if(greatGrandChild.length > 0 && greatGrandChild[0].getText().equals(splitNames[i + 1])) {
                                        return grandChild;
                                    }
                                }
                            }
                            return null;
                        }
                    }
                }
            }
            if(i == splitNames.length - 1)  { //Last run, this is where i can search for enum definitions and fields
                for (PsiElement child : ref.getChildren()) {
                    if((child.getNode().getElementType() == SchemaParser.ENUM_DEFINITION && child.getChildren()[1].getText().equals(splitNames[i])
                            || (child.getNode().getElementType() == SchemaParser.FIELD_DEFINITION && child.getChildren()[1].getText().equals(splitNames[i])))) {
                        return child;
                    }
                }
            }
            boolean found = false;
            for (PsiElement child : ref.getChildren()) {
                if(child.getNode().getElementType() == SchemaParser.TYPE_DEFINITION) {
                    PsiElement[] childChildren = child.getChildren();
                    if(childChildren.length > 1 && childChildren[1].getText().equals(splitNames[i])) {
                        ref = out = child;
                        found = true;
                        break;
                    }
                }
            }
            if(!found) {
                out = null;
                break;
            }
        }


        if(out == null) { //Search nested named
            PsiElement parent = element.getParent();
            while (!(parent instanceof PsiFile)) {
                if(parent.getNode().getElementType() == SchemaParser.TYPE_DEFINITION) {
                    for (int i = 2; i < parent.getChildren().length; i++) {
                        if(parent.getChildren()[i].getNode().getElementType() == SchemaParser.TYPE_DEFINITION) {
                            if(parent.getChildren()[i].getChildren()[1].getText().equals(elementName))  {
                                return parent.getChildren()[i];
                            }
                        }
                    }
                }
                parent = parent.getParent();
            }
        }

        if(out == null && searchImports) { //Search through other dirs
            PsiDirectory parent = element.getContainingFile().getParent();
            while (parent != null) { //Locate the absolute root element
                if(parent.getParent() == null) {
                    break;
                }
                parent = parent.getParent();
            }
            if(parent != null) {
                Set<String> imports = Sets.newHashSet();
                for (PsiElement child : element.getContainingFile().getChildren()) {
                    if(child.getNode().getElementType() == SchemaParser.IMPORT_DEFINITION) {
                        imports.add(child.getChildren()[1].getText());//String[] elements = .replace("\"", "").split("/");
                    }
                }

                for (Module module : ModuleManager.getInstance(element.getProject()).getSortedModules()) {
                    for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
                        if(contentRoot.getPresentableUrl().endsWith("\\schema")) {
                            PsiElement e = searchFolder(PsiManager.getInstance(module.getProject()).findDirectory(contentRoot), elementName, imports);
                            if(e != null) {
                                return e;
                            }
                        }
                    }
                }
                for (VirtualFile sourceRoot : ProjectRootManager.getInstance(element.getProject()).getContentSourceRoots()) {
                    if(sourceRoot.getPresentableUrl().endsWith("\\schema")) {
                        PsiElement e = searchFolder(PsiManager.getInstance(element.getProject()).findDirectory(sourceRoot), elementName, imports);
                        if(e != null) {
                            return e;
                        }
                    }
                }
            }
        }
        return out;
    }

    public static PsiElement searchFolder(@Nullable PsiDirectory folder, String elementName, Set<String> imports) {
        if(folder != null) {
            for (String s : imports) {
                String[] elements = s.replace("\"", "").split("/");
                PsiDirectory reference = folder;
                boolean donethrough = true;
                for (int i = 0; i < elements.length - 1; i++) {
                    if(reference == null) {
                        donethrough = false;
                        break;
                    }
                    reference = reference.findSubdirectory(elements[i]);
                }
                if(donethrough && reference != null) {
                    PsiFile file = reference.findFile(elements[elements.length - 1]);
                    if(file != null) {
                        for (PsiElement fileChild : file.getChildren()) {
                            if(fileChild.getNode().getElementType() == SchemaParser.PACKAGE_DEFINITION) {
                                String packname = fileChild.getChildren()[1].getText();
                                if(elementName.startsWith(packname)) {
                                    return resolveElement(fileChild, elementName.substring(packname.length() + 1), false);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<PsiElement> getOrderedFields(PsiElement obj) {
        Map<Integer, PsiElement> elementMap = new HashMap<>();
        int max = -1;
        for (PsiElement child : obj.getChildren()) {
            if(child.getNode().getElementType() == SchemaParser.FIELD_DEFINITION) {
                int i = Integer.parseInt(child.getChildren()[2].getText());
                max = Math.max(max, i + 1);
                elementMap.put(i, child);
            }
        }
        List<PsiElement> list = new LinkedList<>();
        for (int i = 0; i < max; i++) {
            if(elementMap.containsKey(i)) { //Should always be true?
                list.add(elementMap.get(i));
            }
        }
        return list;
    }

}