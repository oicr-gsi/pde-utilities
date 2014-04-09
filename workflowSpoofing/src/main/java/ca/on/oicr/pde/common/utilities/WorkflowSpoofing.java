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

public class WorkflowSpoofing {
    
    public void run(String xmlPath) throws DocumentException, IOException{
        List<String> scripts = parseWorkflowXML(xmlPath);
        getOutputFiles(scripts);
        
    }
    
    public List<String> getOutputFiles(List<String> scripts) throws IOException{
        
        List<String> outputFiles = new ArrayList<String>();
        
        for (String scriptPath : scripts){
            File scriptFile = new File(scriptPath);
            
            String contents = FileUtils.readFileToString(scriptFile);
            for(String s : contents.split("--")){
                if(s.matches("output-file.*")){
                    outputFiles.add(s.substring(s.indexOf(" "), s.lastIndexOf(" ")));
                    
                }
            }
        }
        return outputFiles;
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

            if (attribute.getValue().matches("provisionFile_out_.*")) {
                return true;
            }
        }
        return false;

    }

    private String getOutputScriptFilePaths(Element action) {
        for(Iterator i = action.elementIterator("sge"); i.hasNext();){
            Element sge = (Element) i.next();
            for(Iterator j = sge.elementIterator("script"); j.hasNext();){
                Element scriptPath = (Element) j.next();
                String path = scriptPath.getData().toString();
                return path;
            }
            
        }
        return null;
    }

    public static void main(String[] args) throws DocumentException, IOException {
        WorkflowSpoofing ws = new WorkflowSpoofing();
        ws.run("/home/rsuri/working/oozie-f04a4fd7-60fa-4871-8904-3b486642ec94/workflow.xml");
    }

}
