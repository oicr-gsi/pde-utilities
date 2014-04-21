/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.on.oicr.pde.common.utilities;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

/**
 *
 * @author Raunaq Suri
 */
class FileProvenanceReaderForFileLinker {

    private static CellProcessor[] getProcessors() {
        final CellProcessor[] processers = new CellProcessor[]{
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            new NotNull(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        };

        return processers;
    }

    private static String[] getHeader() {
        String header[] = {
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "sampleName",
            null,
            null,
            null,
            null,
            "runName",
            null,
            null,
            null,
            null,
            null,
            "lane",
            null,
            null,
            null,
            "ius",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        };
        return header;
    }

    public static Set<SpoofLinker> readWithCsvMapReader(StringReader reader) throws Exception {

        ICsvBeanReader beanReader = null;
        Set<SpoofLinker> fileLinkerObjects = new HashSet<SpoofLinker>();

        try {
            beanReader = new CsvBeanReader(reader, CsvPreference.TAB_PREFERENCE);

            beanReader.getHeader(true); //Skip the header
            final CellProcessor[] processors = getProcessors();
            final String[] header = getHeader();

            SpoofLinker fileLinker;

            while ((fileLinker = beanReader.read(SpoofLinker.class, header, processors)) != null) {
                fileLinkerObjects.add(fileLinker);
                
            }
            

        } finally {
            if (beanReader != null) {
                beanReader.close();
            }
        }

        return fileLinkerObjects;

    }
}
