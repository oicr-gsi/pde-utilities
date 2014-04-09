package ca.on.oicr.pipedev.common.utilities;

/**
 * @author Raunaq Suri
 *
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class WorkflowSpoofing 
{
    public List<String> parseWorkflowXML(String xmlPath) throws DocumentException{
        List<String> outputScripts = new ArrayList<String>();
        SAXReader reader = new SAXReader();
        Document document = reader.read(xmlPath);
        
        //Gets root element
        Element root = document.getRootElement();
        
       //iterates through all the <action> tags
        for( Iterator i = root.elementIterator("action"); i.hasNext(); ){
            Element action = (Element) i.next();
            
            //Iterates through action to verify that the action is an out
            
        }
        return null;
    }
    
    
    public static void main( String[] args ) throws DocumentException
    {
        WorkflowSpoofing ws = new WorkflowSpoofing();
        ws.parseWorkflowXML("/home/rsuri/working/oozie-f04a4fd7-60fa-4871-8904-3b486642ec94/workflow.xml");
    }
}
