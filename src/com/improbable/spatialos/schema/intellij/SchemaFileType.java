package com.improbable.spatialos.schema.intellij;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class SchemaFileType extends LanguageFileType {
    public static final SchemaFileType SCHEMA_FILE_TYPE = new SchemaFileType();

    private SchemaFileType(){
        super(SchemaLanguage.SCHEMA_LANGUAGE);
    }

    @Override
    public @NotNull String getName() {
        return "Schema";
    }

    @Override
    public @NotNull String getDescription() {
        return "SpatialOS schema file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "schema";
    }

    @Override
    public Icon getIcon() {
        return SchemaIcons.FILE_TYPE;
    }
}
