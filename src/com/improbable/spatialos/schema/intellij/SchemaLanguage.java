package com.improbable.spatialos.schema.intellij;

import com.intellij.lang.Language;

public class SchemaLanguage extends Language {
    public static final Language SCHEMA_LANGUAGE = new SchemaLanguage();
    public static final String LANGUAGE_ID = "SpatialOS Schema";

    SchemaLanguage() {
        super(LANGUAGE_ID);
    }
}
