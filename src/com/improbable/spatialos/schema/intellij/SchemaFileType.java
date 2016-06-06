package com.improbable.spatialos.schema.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;

import javax.swing.Icon;

public class SchemaFileType extends LanguageFileType {
    public static final SchemaFileType SCHEMA_FILE_TYPE = new SchemaFileType();
    public static final String[] DEFAULT_ASSOCIATED_EXTENSIONS = new String[]{SCHEMA_FILE_TYPE.getDefaultExtension()};

    private SchemaFileType(){
        super(SchemaLanguage.SCHEMA_LANGUAGE);
    }

    @Override
    public String getName() {
        return "Schema";
    }

    @Override
    public String getDescription() {
        return "SpatialOS schema file";
    }

    @Override
    public String getDefaultExtension() {
        return "schema";
    }

    @Override
    public Icon getIcon() {
        return SchemaIcons.FILE_TYPE;
    }
}
