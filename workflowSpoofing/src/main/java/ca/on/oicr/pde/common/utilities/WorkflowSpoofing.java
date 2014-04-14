package ca.on.oicr.pde.common.utilities;

/**
 * @author Raunaq Suri
 *
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import ca.on.oicr.pde.deciders.BamQCDecider;
import java.io.FilenameFilter;
import java.util.Arrays;

public class WorkflowSpoofing {

    public void run(String xmlPath) throws DocumentException, IOException {
        //Create the file linker file
        File fileLinkerFile = new File("fileLinkerFile");
        //Writes the file's header
        FileUtils.writeStringToFile(fileLinkerFile, "sequencer_run,sample,lane,ius_sw_accession,file_status,mime_type,file\n");
        fileLinkerFile.deleteOnExit();

        List<String> scripts = parseWorkflowXML(xmlPath);
        parseProvisionScripts(scripts);
        List<File> iniFiles = new ArrayList<File>();

        //Testing the parsing of the ini files
        File iniRoot = new File("/tmp");

        FilenameFilter filefilter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.matches(".*.ini")) {
                    return true;
                }
                return false;
            }
        };
        iniFiles.addAll(Arrays.asList(iniRoot.listFiles(filefilter)));

        for (File f : iniFiles) {
            parseIniFile(f.getCanonicalPath());
            System.out.println(f.getCanonicalPath());
        }
    }

    public void parseProvisionScripts(List<String> scripts) throws IOException {
        String outputFile = ".";
        String mimeType = ".";
        List<String> parentAccessions = new ArrayList<String>();

        for (String scriptPath : scripts) {
            File scriptFile = new File(scriptPath);
            String contents = FileUtils.readFileToString(scriptFile);
            for (String individualCommand : splitIntoCommands(contents)) {
                for (String parameter : splitIntoParameters(individualCommand)) {
                    if (parameter.matches("output-file .*")) {
                        outputFile = getOutputFile(parameter);
                    } else if (parameter.matches("input-file-metadata .*")) {
                        mimeType = getMimeType(parameter);
                    }

                }
            }

        }
    }

    private String getMimeType(String parameter) {
        return parameter.substring(parameter.indexOf("::"), parameter.lastIndexOf("::"));
    }

    private String getOutputFile(String parameter) {
        return parameter.substring(parameter.indexOf(" "), parameter.lastIndexOf(" "));
    }

    private String[] splitIntoCommands(String contents) {
        return contents.split("\n");
    }

    private String[] splitIntoParameters(String contents) {
        return contents.split("--");
    }

    public String parseIniFile(String iniFilePath) throws IOException {
        File iniFile = new File(iniFilePath);
        String iniToString = FileUtils.readFileToString(iniFile);
        //Splits the file by newlines

        for (String s : iniToString.split("\n")) {
            if (s.matches("^parent[-_]accession[s]*.*$")) {
                System.out.println(s.substring(s.indexOf("=") + 1));
                return s.substring(s.indexOf("=") + 1);
            }
        }
        return null;
    }

    public List<String> parseWorkflowXML(String xmlPath) throws DocumentException {
        List<String> outputScripts = new ArrayList<String>();

        List<Element> actionOut = new ArrayList<Element>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(xmlPath);

        //Gets root element
        Element root = document.getRootElement();

        //iterates through all the <action> tags
        for (Iterator i = root.elementIterator("action"); i.hasNext();) {
            Element action = (Element) i.next();

            //Iterates through action to verify that the action is an out
            if (isProvisionOut(action)) {
                actionOut.add(action);
            }
        }
        for (Element action : actionOut) {
            outputScripts.add(getOutputScriptFilePaths(action));
        }
        return outputScripts;
    }

    private boolean isProvisionOut(Element action) {

        for (Iterator i = action.attributeIterator(); i.hasNext();) {
            Attribute attribute = (Attribute) i.next();

            if (attribute.getValue().matches("provisionFile_[oO]ut_.*")) {
                return true;
            }
        }
        return false;

    }

    private String getOutputScriptFilePaths(Element action) {
        for (Iterator i = action.elementIterator("sge"); i.hasNext();) {
            Element sge = (Element) i.next();
            for (Iterator j = sge.elementIterator("script"); j.hasNext();) {
                Element scriptPath = (Element) j.next();
                String path = scriptPath.getData().toString();
                return path;
            }

        }
        return null;
    }

    public static void main(String[] args) throws DocumentException, IOException {
        WorkflowSpoofing ws = new WorkflowSpoofing();
        String[] bamParams = {"--wf-accession", "928", "--study-name", "PDE_TEST", "--schedule"};
        BamQCDecider.main(bamParams);
//        ws.run("/.mounts/labs/PDE/public/rsuri/working/oozie-f0346d6f-04ff-42da-9782-548139e78361/workflow.xml");
    }

}
