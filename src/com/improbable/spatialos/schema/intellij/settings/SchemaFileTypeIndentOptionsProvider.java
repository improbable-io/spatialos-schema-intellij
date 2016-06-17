package com.improbable.spatialos.schema.intellij.settings;

import com.improbable.spatialos.schema.intellij.SchemaFileType;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.FileTypeIndentOptionsProvider;

public class SchemaFileTypeIndentOptionsProvider implements FileTypeIndentOptionsProvider {
    @Override
    public CommonCodeStyleSettings.IndentOptions createIndentOptions() {
        CommonCodeStyleSettings.IndentOptions options = new CommonCodeStyleSettings.IndentOptions();
        options.TAB_SIZE = 2;
        options.INDENT_SIZE = 2;
        options.CONTINUATION_INDENT_SIZE = 4;
        return options;
    }

    @Override
    public FileType getFileType() {
        return SchemaFileType.SCHEMA_FILE_TYPE;
    }

    @Override
    public IndentOptionsEditor createOptionsEditor() {
        return null;
    }

    @Override
    public String getPreviewText() {
        return null;
    }

    @Override
    public void prepareForReformat(PsiFile psiFile) {}
}
