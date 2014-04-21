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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
import net.sourceforge.seqware.pipeline.plugins.FileLinker;
import net.sourceforge.seqware.pipeline.plugins.WorkflowLauncher;
import net.sourceforge.seqware.pipeline.runner.PluginRunner;
import org.apache.commons.io.IOUtils;

public class WorkflowSpoofing {

    private final List<String> scripts = new ArrayList<String>();
    private final Set<String> parentAccessions = new HashSet<String>();
    private String mimeType;
    private String outputFile;
    private final Map<String, String> hm = ConfigTools.getSettings();
    private final Metadata metadata = MetadataFactory.get(hm);

    public void spoof(int workflowRunID) throws Exception {

        int wfAccession = metadata.getWorkflowRun(workflowRunID).getWorkflowAccession();
        String bundle = metadata.getWorkflow(wfAccession).getCwd();
        
        File iniFile = new File("tempIniFile");
        FileUtils.writeStringToFile( iniFile ,metadata.getWorkflowRun(workflowRunID).getIniFile());
        iniFile.deleteOnExit();
        
        PrintStream old = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dryRun(wfAccession, iniFile.getCanonicalPath());
        
        String dryRunOut = baos.toString();
        String oozieDir = getOozieDir(dryRunOut);
        //parses the ini to get all the parent accessions the workflow accesses
        parseIniFile(metadata.getWorkflowRun(workflowRunID).getIniFile());

        //Create the file linker file
        File fileLinkerFile = new File("fileLinkerFile.csv");
        //Writes the file's header
        FileUtils.writeStringToFile(fileLinkerFile, "sequencer_run,sample,lane,ius_sw_accession,file_status,mime_type,file\n");
        fileLinkerFile.deleteOnExit();

        //Gets all the provision File out scripts
        scripts.addAll(parseWorkflowXML(oozieDir+"/workflow.xml"));

        //Casts the list into a set
        //Gets the file provenance report and stores it in file linker objects and write it to the file linker file
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

        //runs the file linker plugin
        //runFileLinkerPlugin(fileLinkerFile.getCanonicalPath(), String.valueOf(wfAccession));
    }

    private void runFileLinkerPlugin(String fileLinkerPath, String wfaccession) throws IOException {

        String[] fileLinkerParams = {"--file-list-file", fileLinkerPath, "--workflow-accession", wfaccession, "--csv-separator", ","};
        PluginRunner p = new PluginRunner();
        List<String> a = new ArrayList<String>();
        a.add("--plugin");
        a.add(FileLinker.class.getCanonicalName());
        a.add("--");
        a.addAll(Arrays.asList(fileLinkerParams));
        System.out.println(Arrays.deepToString(a.toArray()));

        p.run(a.toArray(new String[a.size()]));
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

    public void parseIniFile(String iniFile) throws IOException {
        //Splits the file by newlines
        String accessions = "";
        for (String s : iniFile.split("\n")) {
            if (s.matches("^parent[-_]accession[s]*.*$")) {
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
        ws.spoof(929);
    }

    private void dryRun(int workflowAccession, String iniFilePath) {
        String[] fileLinkerParams = {"--workflow-accession", String.valueOf(workflowAccession),
            "--ini-files", iniFilePath, "--no-metadata", "--no-run"};
        PluginRunner p = new PluginRunner();
        List<String> a = new ArrayList<String>();
        a.add("--plugin");
        a.add(WorkflowLauncher.class.getCanonicalName());
        a.add("--");
        a.addAll(Arrays.asList(fileLinkerParams));
        System.out.println(Arrays.deepToString(a.toArray()));

        p.run(a.toArray(new String[a.size()]));
    }

    private String getOozieDir(String dryRunOut) throws Exception {
        for(String s : dryRunOut.split("\n")){
            if(s.matches("^Using working directory:.*$")){
                String[] t = s.split(": ");
                return t[1];
            }
        }
        throw new Exception("Ooozie Dir not found");
    }
}
