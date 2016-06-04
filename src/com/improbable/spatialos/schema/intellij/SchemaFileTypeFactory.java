package com.improbable.spatialos.schema.intellij;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class SchemaFileTypeFactory extends FileTypeFactory {
    public SchemaFileTypeFactory() {
        super();
    }

    @Override
    public void createFileTypes(FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(SchemaFileType.SCHEMA_FILE_TYPE,
                                 SchemaFileType.SCHEMA_FILE_TYPE.getDefaultExtension());
    }
}
