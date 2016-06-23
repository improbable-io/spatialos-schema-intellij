package com.improbable.spatialos.schema.intellij.settings;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SchemaProjectConfigurable implements Configurable {
    private SchemaProjectProperties properties;
    private Gui gui = null;

    SchemaProjectConfigurable(Project project) {
        properties = ServiceManager.getService(project, SchemaProjectProperties.class);
    }

    @Override
    public String getDisplayName() {
        return SchemaLanguage.LANGUAGE_ID;
    }

    @Override
    public @Nullable String getHelpTopic() {
        return null;
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (gui == null) {
            gui = new Gui();
        }
        return gui;
    }

    @Override
    public boolean isModified() {
        return gui != null && !properties.getState().schemaPaths.equals(
                SchemaProjectProperties.parseSchemaPaths(gui.getSchemaPaths()));
    }

    @Override
    public void apply() throws ConfigurationException {
        if (gui != null) {
            properties.setSchemaPaths(gui.getSchemaPaths());
        }
    }

    @Override
    public void reset() {
        if (gui != null) {
            gui.setSchemaPaths(SchemaProjectProperties.formatSchemaPaths(properties.getState().schemaPaths));
        }
    }

    @Override
    public void disposeUIResources() {
    }

    private static class Gui extends JPanel {
        private JTextArea schemaPaths = new JTextArea();

        public Gui() {
            setLayout(new GridLayoutManager(1, 2));
            setRequestFocusEnabled(true);

            schemaPaths.setAutoscrolls(true);
            schemaPaths.setEditable(true);
            schemaPaths.setEnabled(true);
            JLabel schemaPathsLabel = new JLabel();
            schemaPathsLabel.setText("Schema paths (one per line):");
            schemaPathsLabel.setLabelFor(schemaPaths);

            add(schemaPathsLabel, new GridConstraints(
                    0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, 0,
                    GridConstraints.SIZEPOLICY_CAN_SHRINK, 0, null, null, null));

            add(schemaPaths, new GridConstraints(
                    0, 1, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        }

        public String getSchemaPaths() {
            return schemaPaths.getText();
        }

        public void setSchemaPaths(String value) {
            schemaPaths.replaceRange(value, 0, schemaPaths.getText().length());
        }
    }
}
