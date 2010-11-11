package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import org.limewire.util.URIUtils;

/**
 * Allow various Magnet Related HTML page rendering.
 */
public class MagnetHTML {

    static String buildMagnetDetailPage(String cmd) throws IOException {
        StringTokenizer st = new StringTokenizer(cmd, "&");
        String keystr;
        String valstr;
        int    start;
        String address = "";
        String fname   = "";
        String sha1    = "";
        String ret= magnetDetailPageHeader();
        
        // Process each key=value pair
        while (st.hasMoreTokens()) {
            keystr = st.nextToken();
            keystr = keystr.trim();
            start  = keystr.indexOf("=");
            if(start == -1) {
                throw new IOException("invalid command: "+cmd);
            } else {
                start++;
            }
            valstr = keystr.substring(start);
            keystr = keystr.substring(0,start-1);
            try {
                valstr= URIUtils.decodeToUtf8(valstr);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }   
            if ( keystr.equals("addr") ) {
                address = valstr;
            } else if ( keystr.startsWith("n") ) {
                fname = valstr;
            } else if ( keystr.startsWith("u") ) {
                sha1 = valstr;
                ret += magnetDetail(address, fname, sha1);
            }
        }
        ret += 
          "</table>"+
          "</body></html>";
        return ret;
    }

    private static String magnetDetail(String address, String fname, String sha1) {
        String ret =
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Name</b></td>"+
         "    <td bgcolor=\"#FFFFFF\" class=\"name\">"+fname+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>SHA1</b></td>"+
         "    <td bgcolor=\"#ffffff\" class=\"text\">"+sha1+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Link</b></td>"+
         "    <td bgcolor=\"#ffffff\" class=\"text\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+
         fname+"</a></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Magnet</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textarea name=\"textarea\" cols=\"80\" rows=\"4\" wrap=\"VIRTUAL\" class=\"area\">magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"</textarea></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Html link</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textarea name=\"textarea\" cols=\"80\" rows=\"5\" wrap=\"VIRTUAL\" class=\"area\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+fname+"</a></textarea></td>"+
         "  </tr>"+
         "  <tr bgcolor=\"#333333\"> "+
         "    <td colspan=\"2\" class=\"text\" height=\"5\"></td></tr>";

        return ret;
    }


    private static String magnetDetailPageHeader() {
       String ret= 
         "<html>"+
         "<head>"+
         "<title>LimeWire Magnet Descriptions</title>"+
         "<style type=\"text/css\">"+
         "<!--"+
         ".text {"+
         "    font-family: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    color: #333333;"+
         "}"+
         ".header {"+
         "    font-family: Arial, Helvetica, sans-serif;"+
         "    font-size: 14pt;"+
         "    color: #ffffff;"+
         "}"+
         ".name {"+
         "    font-family: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    font-weight: bold;"+
         "    color: #000000;"+
         "}"+
         ".area  { "+
         "border: 1px solid;"+
          "margin: 0;"+
          "padding: 4px;"+
          "background: #FFFEF4;"+
          "color: #333333;"+
          "font: 11px Verdana, Arial;"+
          "text-align: left;"+
         "}"+
         "-->"+
         "</style>"+
         "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"+
         "</head>"+
         "<body bgcolor=\"#666666\">"+
         "<span class=\"header\"><center>"+
         "  LimeWire Magnet Details "+
         "</center></span><br>"+
         "<table border=\"0\" cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#999999\" align=\"center\">";

        return ret;
    }

}

