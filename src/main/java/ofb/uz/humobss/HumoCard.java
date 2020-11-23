package ofb.uz.humobss;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@RestController
@RequestMapping(value = "/humo")
public class HumoCard {
    private static class NullHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }


    @PostMapping(value = "/getBalance")
    @ResponseBody
    public  String getBalance(@RequestParam String cardNumber){
        System.out.println(cardNumber);
        // connection
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                // or you can return null too
                return new X509Certificate[0];
            }
        }};

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            System.out.println("Passed 1");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String string, SSLSession sslSession) {
                return true;
            }
        });
        //end connection

        try {
            //String cardNumber = "9860270101631892";
            String responseString = "";
            String outputString = "";
            String wsURL = "https://192.168.35.22:6677/";
            URL url = new URL(wsURL);
            URLConnection connection = url.openConnection();
            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());
            HttpURLConnection httpConn = (HttpURLConnection) connection;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            String xmlInput = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:IIACardServices\">\n" +
                    "                   <soapenv:Header/>\n" +
                    "                   <soapenv:Body>\n" +
                    "                   <urn:getCardAccountsBalance>\n" +
                    "                   <primaryAccountNumber>" + cardNumber + "</primaryAccountNumber>\n" +
                    "                   </urn:getCardAccountsBalance>\n" +
                    "                   </soapenv:Body>\n" +
                    "                   </soapenv:Envelope>";
            byte[] buffer = new byte[xmlInput.length()];
            buffer = xmlInput.getBytes();
            bout.write(buffer);
            byte[] b = bout.toByteArray();
            String SOAPAction = "";
            httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
            httpConn.setRequestProperty("Authorization", "Basic d3NvZmI6MUYyZUp4QG0=");
            httpConn.setRequestProperty("Content-Type", "text/xml;charset=utf-8");
            httpConn.setRequestProperty("SOAPAction", SOAPAction);
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            OutputStream out = httpConn.getOutputStream();
            //Write the content of the request to the outputstream of the HTTP Connection.
            out.write(b);
            out.close();
            //Ready with sending the request.

            //Read the response.
            InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
            BufferedReader in = new BufferedReader(isr);

            while ((responseString = in.readLine()) != null) {
                outputString = outputString + responseString;
            }
            Document document = parseXmlFile(outputString);
            NodeList nodeList = document.getElementsByTagName("availableAmount");
            Double balanceResult = Double.parseDouble(nodeList.item(1).getTextContent());

            return   Double.toString(balanceResult/100);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
