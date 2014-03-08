package com.metodica.wemoplugin;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Jacob on 8/5/13.
 */
public class WeMoDevice {
    static final String LOG_TAG = "WEMO_DEVICE";
    private String setupURL = null;
    private String soapURLBase = null;
    private InetAddress WeMoIP = null;
    private int WeMoPort = 49153;
    private String name = "NO_NAME";
    private String type = "NO_TYPE";
    private int status = -1;
    private Document initXML = null;
    private NodeList servicesList = null;

    private String COMMAND_OFF = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:SetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"><BinaryState>0</BinaryState></u:SetBinaryState></s:Body></s:Envelope>";
    private String COMMAND_ON = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:SetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"><BinaryState>1</BinaryState></u:SetBinaryState></s:Body></s:Envelope>";

    public WeMoDevice(String URL) {
        try {
            setupURL = URL;

            setIPFromSetupUrl();
            initXML = getDocumentFromURL();
            parseXMLWeMo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isError() {
        return (status == -1);
    }



    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void sendCommandON() {
        String[] params = new String[1];
        params[0] = COMMAND_ON;
        sendCommand("urn:Belkin:service:basicevent:1", "SetBinaryState", params);
    }

    public void sendCommandOFF() {
        String[] params = new String[1];
        params[0] = COMMAND_OFF;
        sendCommand("urn:Belkin:service:basicevent:1", "SetBinaryState", params);
    }

    /////////////////////\\\\\\\\\\\\\\\\\\\\\\\

    ////           PRIVATE ZONE             \\\\

    /////////////////////\\\\\\\\\\\\\\\\\\\\\\\

    private Document getDocumentFromURL() throws IOException, ParserConfigurationException, SAXException {
        HttpGet uri = new HttpGet(setupURL);

        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse resp = client.execute(uri);

        StatusLine status = resp.getStatusLine();
        if (status.getStatusCode() != 200) {
            Log.d(LOG_TAG, "HTTP error, invalid server status code: " + resp.getStatusLine());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(resp.getEntity().getContent());

        return doc;
    }

    private void setIPFromSetupUrl() {
        String ip = null;
        try {
            InetAddress address = InetAddress.getByName(new URL(setupURL).getHost());
            WeMoIP = address;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void parseXMLWeMo() {
        try {
            NodeList templist = initXML.getElementsByTagName("device");
            NodeList rootNodes = templist.item(0).getChildNodes();

            for (int i=0; i<rootNodes.getLength(); i++)
            {
                if (rootNodes.item(i).getNodeName().equalsIgnoreCase("#text")) continue;

                else if (rootNodes.item(i).getNodeName().equalsIgnoreCase("friendlyName"))
                    setName(innerXml(rootNodes.item(i)));

                else if (rootNodes.item(i).getNodeName().equalsIgnoreCase("deviceType")) {
                    setType(innerXml(rootNodes.item(i)));
                }

                else if (rootNodes.item(i).getNodeName().equalsIgnoreCase("binaryState")) {
                    try {
                        setStatus(Integer.parseInt(innerXml(rootNodes.item(i))));
                    }  catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                else if (rootNodes.item(i).getNodeName().equalsIgnoreCase("serviceList")) {
                    try {
                        this.servicesList = rootNodes.item(i).getChildNodes();
                    }  catch (Exception e) {
                        this.status = -1;
                        e.printStackTrace();
                    }
                }
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sendCommand(String service, String command, String[] params) {
        try {
            if (servicesList == null) throw new Exception ("WeMo services not loaded");
            soapURLBase = "http://" + WeMoIP.getHostAddress() + ":" + WeMoPort;

            Node s = locateNodeByValue(service, servicesList);
            if (s == null) throw new Exception ("WeMo service not found");

            Node controlURL = locateNodeByName("controlURL", s.getParentNode().getChildNodes());
            if (controlURL == null) throw new Exception ("WeMo service error");

            String soapURL = soapURLBase + innerXml(controlURL);
            Log.d(LOG_TAG, "SOAP URL: " + soapURL);

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(soapURL);
            httppost.addHeader("SOAPAction", "\"" + service + "#" + command + "\"");

            try {
                StringEntity se = new StringEntity(params[0]);
                se.setContentType("text/xml");
                se.setContentEncoding("utf-8");
                httppost.setEntity(se);
                HttpResponse response = httpclient.execute(httppost);

                Log.d(LOG_TAG, convertStreamToString(response.getEntity().getContent()));

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Node locateNodeByName (String nodeName, NodeList list) {
        return LocateNodeRecursive(nodeName, list, false);
    }

    private Node locateNodeByValue (String name, NodeList list) {
        return LocateNodeRecursive(name, list, true);
    }

    private Node LocateNodeRecursive(String name, NodeList list, boolean byValue) {
        Node s = null;
        for (int i=0; i<list.getLength(); i++) {
            if (list.item(i).getNodeName().equalsIgnoreCase("#text")) continue;

            else {
                if (byValue) {
                    if (innerXml(list.item(i)).equalsIgnoreCase(name)) s = list.item(i);
                } else {
                    if (list.item(i).getNodeName().equalsIgnoreCase(name)) s = list.item(i);
                }
                if (s == null && list.item(i).hasChildNodes())
                    s = LocateNodeRecursive(name, list.item(i).getChildNodes(), byValue);

            }

            if (s != null) break;
        }
        return s;
    }

    private Node locateNode (String name, NodeList list, boolean byValue) {
        Node s = null;
        for (int i=0; i<servicesList.getLength(); i++) {
            if (list.item(i).getNodeName().equalsIgnoreCase("#text")) continue;

            else {
                if (byValue) {
                    if (innerXml(list.item(i)).equalsIgnoreCase(name)) s = list.item(i);
                } else {
                    if (list.item(i).getNodeName().equalsIgnoreCase(name)) s = list.item(i);
                }
            }
        }
        return s;
    }

    public String innerXml(Node node){
        String s = "";
        NodeList childs = node.getChildNodes();
        for( int i = 0;i<childs.getLength();i++ ){
            s+= serializeNode(childs.item(i));
        }
        return s;
    }

    private String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    private String serializeNode(Node node){
        String s = "";
        if( node.getNodeName().equals("#text") ) return node.getTextContent();
        s+= "<" + node.getNodeName()+" ";
        NamedNodeMap attributes = node.getAttributes();
        if( attributes!= null ){
            for( int i = 0;i<attributes.getLength();i++ ){
                s+=attributes.item(i).getNodeName()+"=\""+attributes.item(i).getNodeValue()+"\"";
            }
        }
        NodeList childs = node.getChildNodes();
        if( childs == null || childs.getLength() == 0 ){
            s+= "/>";
            return s;
        }
        s+=">";
        for( int i = 0;i<childs.getLength();i++ )
            s+=serializeNode(childs.item(i));
        s+= "</"+node.getNodeName()+">";
        return s;
    }
}
