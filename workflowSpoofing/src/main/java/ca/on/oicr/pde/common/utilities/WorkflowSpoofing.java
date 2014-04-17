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
import com.cloudera.net.iharder.base64.Base64;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.seqware.common.metadata.Metadata;
import net.sourceforge.seqware.common.metadata.MetadataFactory;
import net.sourceforge.seqware.common.util.configtools.ConfigTools;
import org.apache.commons.io.IOUtils;

public class WorkflowSpoofing {

    private final List<String> scripts = new ArrayList<String>();
    private final Set<String> parentAccessions = new HashSet<String>();
    private String mimeType;
    private String outputFile;
    Map<String, String> hm = ConfigTools.getSettings();
    Metadata metadata = MetadataFactory.get(hm);

    public void run(String xmlPath) throws Exception {
        //parses the ini to get all the parent accessions the workflow accesses
        parseIniFile("/home/rsuri/workflow_cas_2.2_TS_withManualOutput.ini");

        //Create the file linker file
        File fileLinkerFile = new File("fileLinkerFile.csv");
        //Writes the file's header
        FileUtils.writeStringToFile(fileLinkerFile, "sequencer_run,sample,lane,ius_sw_accession,file_status,mime_type,file\n");
        //fileLinkerFile.deleteOnExit();

        //Gets all the provision File out scripts
        scripts.addAll(parseWorkflowXML(xmlPath));

        //Casts the list into a set

        //Gets the file provenance report and stores it in file linker objects
        Set<SpoofLinker> fileLinkerObjs = FileProvenanceReaderForFileLinker.readWithCsvMapReader(getFileProvenanceReport());
        
        for (String script : scripts) {
            parseProvisionScript(script);
            for (SpoofLinker spoof : fileLinkerObjs) {
                spoof.setFile(outputFile);
                spoof.setMimeType(mimeType);
                spoof.setSeparator(",");
                FileUtils.writeStringToFile(fileLinkerFile, spoof.toString(), true);
            }
                 
        }

//        List<File> iniFiles = new ArrayList<File>();
        //Testing the parsing of the ini files
//        File iniRoot = new File("/tmp");
//
//        FilenameFilter filefilter = new FilenameFilter() {
//
//            public boolean accept(File dir, String name) {
//                if (name.matches(".*.ini")) {
//                    return true;
//                }
//                return false;
//            }
//        };
//        iniFiles.addAll(Arrays.asList(iniRoot.listFiles(filefilter)));
//
//        for (File f : iniFiles) {
//            parseIniFile(f.getCanonicalPath());
//            System.out.println(f.getCanonicalPath());
//        }
    }

    private StringReader getFileProvenanceReport() throws Exception {
        String userAuth = Base64.encodeBytes("admin@admin.com:admin".getBytes());
        String url = getWebserviceUrl();
        String params = "?";

        Iterator<String> processingIter = parentAccessions.iterator();

        //First id
        if (processingIter.hasNext()) {
            String id = processingIter.next();
            params += "processing=" + id;
        }
        //The rest
        while (processingIter.hasNext()) {
            String id = processingIter.next();
            params += "&processing=" + id;
        }

        //Removes the last slash of the webservice url if it exists
        if (url.endsWith("/")) {
            url = url.substring(0, url.lastIndexOf("/"));
            System.out.println(url);
        }

        //Refreshes file provenance report
        URL refreshUrl = new URL(url + "/reports/file-provenance/generate");
        HttpURLConnection connection2 = (HttpURLConnection) refreshUrl.openConnection();
        connection2.setRequestMethod("GET");
        connection2.setDoOutput(true);
        connection2.setRequestProperty("Authorization", "Basic " + userAuth);
        connection2.connect();

        //Gets the file provenance report
        URL reportUrl = new URL(url + "/reports/file-provenance" + params);

        HttpURLConnection connection = (HttpURLConnection) reportUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Basic " + userAuth);
        connection.connect();

        StringWriter reportWriter = new StringWriter();
        IOUtils.copy(connection.getInputStream(), reportWriter);      
        StringReader reader = new StringReader(reportWriter.toString().trim());
        return reader;
    }

    private void parseProvisionScript(String script) throws IOException {

        File scriptFile = new File(script);

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

    private String getWebserviceUrl() throws Exception {
        String aScript = FileUtils.readFileToString(new File(scripts.get(0)));
        String settingsPath = "";
        for (String scriptLines : aScript.split("\n")) {
            if (scriptLines.matches("export SEQWARE_SETTINGS=.*")) {
                settingsPath = scriptLines.substring(scriptLines.indexOf("=") + 1);
            }
        }
        if (settingsPath.isEmpty()) {
            throw new Exception("Settings file not found");
        }

        String settings = FileUtils.readFileToString(new File(settingsPath));
        for (String setting : settings.split("\n")) {
            if (setting.matches("SW_REST_URL=.*")) {
                return setting.substring(setting.indexOf("=") + 1);
            }
        }
        throw new Exception("Could not get webservice url");
    }

    private String getMimeType(String parameter) {
        return parameter.substring(parameter.indexOf("::") + 2, parameter.lastIndexOf("::"));
    }

    private String getOutputFile(String parameter) {
        return parameter.substring(parameter.indexOf(" ") + 1, parameter.lastIndexOf(" "));
    }

    private String[] splitIntoCommands(String contents) {
        return contents.split("\n");
    }

    private String[] splitIntoParameters(String contents) {
        return contents.split("--");
    }

    public void parseIniFile(String iniFilePath) throws IOException {
        File iniFile = new File(iniFilePath);
        String iniToString = FileUtils.readFileToString(iniFile);
        //Splits the file by newlines
        String accessions = "";
        for (String s : iniToString.split("\n")) {
            if (s.matches("^parent[-_]accession[s]*.*$")) {
                System.out.println(s.substring(s.indexOf("=") + 1));
                accessions = s.substring(s.indexOf("=") + 1);
            }
        }
        parentAccessions.addAll(Arrays.asList(accessions.split(",")));
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

    public static void main(String[] args) throws Exception {
        WorkflowSpoofing ws = new WorkflowSpoofing();
        // String[] bamParams = {"--wf-accession", "928", "--study-name", "PDE_TEST", "--schedule"};
//        BamQCDecider.main(bamParams);
        ws.run("/home/rsuri/working/oozie-f0346d6f-04ff-42da-9782-548139e78361/workflow.xml");
    }
}
