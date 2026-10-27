package com.example.alek.ble_hid_example;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class StorageFileTest {

    @Test
    public void testBlocklyMacroSaveAndLoadFromFileStorage() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File externalDir = context.getExternalFilesDir(null);

        File macroFile = new File(externalDir, "macro_autosave.xml");

        // Test writing XML content to file storage
        String xmlContent = "<xml xmlns=\"https://developers.google.com/blockly/xml\"><block type=\"hid_type_text\"></block></xml>";
        FileWriter writer = new FileWriter(macroFile);
        writer.write(xmlContent);
        writer.close();

        // Verify file exists on storage
        assertTrue(macroFile.exists());

        // Test reading back XML content from file storage
        StringBuilder readBack = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(macroFile));
        String line;
        while ((line = reader.readLine()) != null) {
            readBack.append(line);
        }
        reader.close();

        assertEquals(xmlContent, readBack.toString());

        // Cleanup
        macroFile.delete();
    }
}
