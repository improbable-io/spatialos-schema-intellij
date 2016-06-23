package com.improbable.spatialos.schema.intellij.settings;

import com.improbable.spatialos.schema.intellij.SchemaLanguage;
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@State(name=SchemaLanguage.LANGUAGE_ID, storages={
        @Storage(id="dir", file=StoragePathMacros.PROJECT_CONFIG_DIR + "/spatial.xml",
                 scheme=StorageScheme.DIRECTORY_BASED)
})
public class SchemaProjectProperties implements PersistentStateComponent<SchemaProjectProperties.State> {
    private State state = new State();

    public static class State {
        public List<String> schemaPaths;

        public State() {
            this.schemaPaths = new ArrayList<>();
        }

        public State(List<String> schemaPaths) {
            this.schemaPaths = schemaPaths;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof State && schemaPaths.equals(((State) other).schemaPaths);
        }

        @Override
        public int hashCode() {
            return schemaPaths.hashCode();
        }
    }

    public void setSchemaPaths(String rawSchemaPaths) {
        state = new State(parseSchemaPaths(rawSchemaPaths));
    }

    public static List<String> parseSchemaPaths(String rawSchemaPaths) {
        List<String> schemaPaths = new ArrayList<>();
        for (String path : rawSchemaPaths.split("\n")) {
            if (!path.equals("")) {
                schemaPaths.add(path);
            }
        }
        return schemaPaths;
    }

    public static String formatSchemaPaths(List<String> schemaPaths) {
        return String.join("\n", schemaPaths);
    }

    @Override
    public @NotNull SchemaProjectProperties.State getState() {
        return state;
    }

    @Override
    public void loadState(SchemaProjectProperties.State state) {
        this.state = state;
    }
}
