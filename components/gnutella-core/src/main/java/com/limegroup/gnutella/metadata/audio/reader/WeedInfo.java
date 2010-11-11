package com.limegroup.gnutella.metadata.audio.reader;



/**
 * Encapsulates information about Weedified files.
 * See http://www.weedshare.com.
 */
public class WeedInfo extends WRMXML {
    
    public static final String LAINFO = "http://www.shmedlic.com/license/3play.aspx";
    public static final String LDIST  = "Shared Media Licensing, Inc.";
    public static final String LURL   = "http://www.shmedlic.com/";
    public static final String CID = " cid: ";
    public static final String VID = " vid: ";
    
    private String _versionId, _contentId, _ice9;
    private String _licenseDate, _licenseDistributor, _licenseDistributorURL;
    private String _publishDate;
    private String _contentDistributor, _contentDistributorURL;
    private String _price, _collection, _description, _copyright;
    private String _artistURL, _author, _title;
    
    /**
     * Constructs a new WeedInfo based off the given WRMXML.
     */
    public WeedInfo(WRMXML data) {
        super(data._documentNode);
        
        //The XML should look something like:
        //<WRMHEADER version="2.0.0.0">
        //    <DATA>
        //        <VersionID>0000000000001370651</VersionID>
        //        <ContentID>214324</ContentID>
        //        <ice9>ice9</ice9>
        //        <License_Date></License_Date>
        //        <License_Distributor_URL>http://www.shmedlic.com/</License_Distributor_URL>
        //        <License_Distributor>Shared Media Licensing, Inc.</License_Distributor>
        //        <Publish_Date>4/14/2005 4:13:50 PM</Publish_Date>
        //        <Content_Distributor_URL>http://www.presidentsrock.com</Content_Distributor_URL>
        //        <Content_Distributor>PUSA Inc.</Content_Distributor>
        //        <Price>0.9900</Price>
        //        <Collection>Love Everybody</Collection>
        //        <Description></Description>
        //        <Copyright>2004 PUSA Inc.</Copyright>
        //        <Artist_URL>http://www.presidentsrock.com</Artist_URL>
        //        <Author>The Presidents of the United States of America</Author>
        //        <Title>Love Everybody</Title>
        //        <SECURITYVERSION>2.2</SECURITYVERSION>
        //        <CID>o9miGn4Z0k2gUeHhN9VxTA==</CID>
        //        <LAINFO>http://www.shmedlic.com/license/3play.aspx</LAINFO>
        //        <KID>ERVOYkZ8qkWZ75OQw9ihnA==</KID>
        //        <CHECKSUM>t1ZpoYJF2w==</CHECKSUM>
        //    </DATA>
        //    <SIGNATURE>
        //        <HASHALGORITHM type="SHA"></HASHALGORITHM>
        //        <SIGNALGORITHM type="MSDRM"></SIGNALGORITHM>
        //        <VALUE>XZkWZWCq919yum!bBGdxvnpiS38npAqAofxT8AkegyJ27zTlb9v4gA==</VALUE>
        //    </SIGNATURE>
        //</WRMHEADER> 
    }
    
    /**
     * Determines if this WeedInfo is valid.
     */
    @Override
    public boolean isValid() {
        return  LAINFO.equals(_lainfo) &&
                LURL.equals(_licenseDistributorURL) &&
                LDIST.equals(_licenseDistributor) &&
                _contentId != null &&
                _versionId != null;
    }
    
    public String getIce9() { return _ice9; }
    public String getVersionId() { return _versionId; }
    public String getContentId() { return _contentId; }
    public String getLicenseDate() { return _licenseDate; }
    public String getLicenseDistributorURL() { return _licenseDistributorURL; }
    public String getLicenseDistributor() { return _licenseDistributor; }
    public String getPublishDate() { return _publishDate; }
    public String getContentDistributor() { return _contentDistributor; }
    public String getContentDistrubutorURL() { return _contentDistributorURL; }
    public String getPrice() { return _price; }
    public String getCollection() { return _collection; }
    public String getDescription() { return _description; }
    public String getAuthor() { return _author; }
    public String getArtistURL() { return _artistURL; }
    public String getTitle() { return _title; }
    public String getCopyright() { return _copyright; }
    
    public String getLicenseInfo() {
        return _lainfo + CID + _contentId + VID +  _versionId;
    }
    
    /**
     * Extends WRMXML's parseChild to look for Weed-specific elements.
     */
    @Override
    protected void parseChild(String parentNodeName, String name, String attribute, String value) {
        super.parseChild(parentNodeName, name, attribute, value);
        
        if(attribute != null || !parentNodeName.equals("DATA"))
            return;
        
        if(name.equals("VersionID"))
            _versionId = value;
        else if(name.equals("ContentID"))
            _contentId = value;
        else if(name.equals("License_Date"))
            _licenseDate = value;
        else if(name.equals("License_Distributor"))
            _licenseDistributor = value;
        else if(name.equals("License_Distributor_URL"))
            _licenseDistributorURL = value;
        else if(name.equals("Publish_Date"))
            _publishDate = value;
        else if(name.equals("Content_Distributor"))
            _contentDistributor = value;
        else if(name.equals("Content_Distributor_URL"))
            _contentDistributorURL = value;
        else if(name.equals("Price"))
            _price = value;
        else if(name.equals("Collection"))
            _collection = value;
        else if(name.equals("Description"))
            _description = value;
        else if(name.equals("Copyright"))
            _copyright = value;
        else if(name.equals("Artist_URL"))
            _artistURL = value;
        else if(name.equals("Author"))
            _author = value;
        else if(name.equals("Title"))
            _title = value;
        else if(name.equals("ice9"))
            _ice9 = value;
        else if(name.equals("Copyright"))
            _copyright = value;
    }
}