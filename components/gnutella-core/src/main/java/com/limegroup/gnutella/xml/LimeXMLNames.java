package com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Map;

import org.limewire.i18n.I18nMarker;


public class LimeXMLNames {

    public static final String AUDIO_SCHEMA = "http://www.limewire.com/schemas/audio.xsd";
    
    public static final String AUDIO = "audios__audio";
    public static final String AUDIO_TITLE = "audios__audio__title__";
    public static final String AUDIO_TRACK = "audios__audio__track__";
    public static final String AUDIO_ARTIST = "audios__audio__artist__";
    public static final String AUDIO_ALBUM = "audios__audio__album__";
    public static final String AUDIO_GENRE = "audios__audio__genre__";
    public static final String AUDIO_COMMENTS = "audios__audio__comments__";
    public static final String AUDIO_YEAR = "audios__audio__year__";
    public static final String AUDIO_TYPE = "audios__audio__type__";
    public static final String AUDIO_LANGUAGE = "audios__audio__language__";
    public static final String AUDIO_SECONDS = "audios__audio__seconds__";
    public static final String AUDIO_SHA1 = "audios__audio__sha1__";
    public static final String AUDIO_BITRATE = "audios__audio__bitrate__";
    public static final String AUDIO_PRICE = "audios__audio__price__";
    public static final String AUDIO_LINK = "audios__audio__link__";
    public static final String AUDIO_ACTION = "audios__audio__action__";
    public static final String AUDIO_LICENSE = "audios__audio__license__";
    public static final String AUDIO_LICENSETYPE = "audios__audio__licensetype__";
    
    public static final String APPLICATION_SCHEMA = "http://www.limewire.com/schemas/application.xsd";
    
    public static final String APPLICATION = "applications__application";
    public static final String APPLICATION_NAME = "applications__application__name__";
    public static final String APPLICATION_PUBLISHER = "applications__application__publisher__";
    public static final String APPLICATION_PLATFORM = "applications__application__platform__";
    public static final String APPLICATION_LICENSETYPE = "applications__application__licensetype__";
    public static final String APPLICATION_LICENSE = "applications__application__license";
    
    public static final String DOCUMENT_SCHEMA = "http://www.limewire.com/schemas/document.xsd";
    
    public static final String DOCUMENT = "documents__document";
    public static final String DOCUMENT_TITLE = "documents__document__title__";
    public static final String DOCUMENT_TOPIC = "documents__document__topic__";
    public static final String DOCUMENT_AUTHOR = "documents__document__author__";
    public static final String DOCUMENT_LICENSE = "documents__document__license__";
    public static final String DOCUMENT_LICENSETYPE = "documents__document__licensetype__";
    
    public static final String IMAGE_SCHEMA = "http://www.limewire.com/schemas/image.xsd";
    
    public static final String IMAGE = "images__image";
    public static final String IMAGE_TITLE = "images__image__title__";
    public static final String IMAGE_DESCRIPTION = "images__image__description__";
    public static final String IMAGE_ARTIST = "images__image__artist__";
    public static final String IMAGE_LICENSE = "images__image__license__";
    public static final String IMAGE_LICENSETYPE = "images__image__licensetype__";
    
    public static final String VIDEO_SCHEMA = "http://www.limewire.com/schemas/video.xsd";
    
    public static final String VIDEO = "videos__video";
    public static final String VIDEO_TITLE = "videos__video__title__";
    public static final String VIDEO_TYPE = "videos__video__type__";
    public static final String VIDEO_YEAR = "videos__video__year__";
    public static final String VIDEO_RATING = "videos__video__rating__";
    public static final String VIDEO_LENGTH = "videos__video__length__";
    public static final String VIDEO_COMMENTS = "videos__video__comments__";
    public static final String VIDEO_LICENSE = "videos__video__license__";
    public static final String VIDEO_LICENSETYPE = "videos__video__licensetype__";
    public static final String VIDEO_HEIGHT = "videos__video__height__";
    public static final String VIDEO_WIDTH = "videos__video__width__";
    public static final String VIDEO_BITRATE = "videos__video__bitrate__";
    public static final String VIDEO_ACTION = "videos__video__action__";
    public static final String VIDEO_DIRECTOR = "videos__video__director__";
    public static final String VIDEO_STUDIO = "videos__video__studio__";
    public static final String VIDEO_LANGUAGE = "videos__video__language__";
    public static final String VIDEO_STARS = "videos__video__stars__";
    public static final String VIDEO_PRODUCER = "videos__video__producer__";
    public static final String VIDEO_SUBTITLES = "videos__video__subtitles__";
    
    
    public static final String TORRENT_SCHEMA = "http://www.limewire.com/schemas/torrent.xsd";
    
    public static final String TORRENT_INFO_HASH = "torrents__torrent__infohash__";
    public static final String TORRENT_TRACKERS = "torrents__torrent__trackers__";
    public static final String TORRENT_LENGTH = "torrents__torrent__length__";
    public static final String TORRENT_NAME = "torrents__torrent__name__";
    public static final String TORRENT_PRIVATE = "torrents__torrent__private__";
    public static final String TORRENT_WEBSEEDS = "torrents__torrent__webseeds__";
    public static final String TORRENT_FILE_PATHS = "torrents__torrent__filepaths__";
    public static final String TORRENT_FILE_SIZES = "torrents__torrent__filesizes__";

    

    private static Map<String, String> displayNameByKey;
    
    public synchronized static String getDisplayName(String key) {
        if (displayNameByKey == null) {
            displayNameByKey = new HashMap<String, String>();
            
            displayNameByKey.put(AUDIO, I18nMarker.marktr("Audio"));
            displayNameByKey.put(AUDIO_TITLE, I18nMarker.marktr("Title"));
            displayNameByKey.put(AUDIO_ARTIST, I18nMarker.marktr("Artist"));
            displayNameByKey.put(AUDIO_ALBUM, I18nMarker.marktr("Album"));
            displayNameByKey.put(AUDIO_GENRE, I18nMarker.marktr("Genre"));
            displayNameByKey.put(AUDIO_TRACK, I18nMarker.marktr("Track"));
            displayNameByKey.put(AUDIO_TYPE, I18nMarker.marktr("Type"));
            displayNameByKey.put(AUDIO_SECONDS, I18nMarker.marktr("Length"));
            displayNameByKey.put(AUDIO_YEAR, I18nMarker.marktr("Year"));
            displayNameByKey.put(AUDIO_LANGUAGE, I18nMarker.marktr("Language"));
            displayNameByKey.put(AUDIO_BITRATE, I18nMarker.marktr("Bitrate"));
            displayNameByKey.put(AUDIO_COMMENTS, I18nMarker.marktr("Comments"));
            displayNameByKey.put(AUDIO_ACTION, I18nMarker.marktr("Action"));
            displayNameByKey.put(AUDIO_LICENSE, I18nMarker.marktr("Copyright"));
            displayNameByKey.put(AUDIO_LICENSETYPE, I18nMarker.marktr("License Type"));

            displayNameByKey.put(VIDEO, I18nMarker.marktr("Video"));
            displayNameByKey.put(VIDEO_TITLE, I18nMarker.marktr("Title"));
            displayNameByKey.put(VIDEO_DIRECTOR, I18nMarker.marktr("Director"));
            displayNameByKey.put(VIDEO_PRODUCER, I18nMarker.marktr("Producer"));
            displayNameByKey.put(VIDEO_STUDIO, I18nMarker.marktr("Studio"));
            displayNameByKey.put(VIDEO_STARS, I18nMarker.marktr("Stars"));
            displayNameByKey.put(VIDEO_TYPE, I18nMarker.marktr("Type"));
            displayNameByKey.put(VIDEO_LENGTH, I18nMarker.marktr("Length"));
            displayNameByKey.put(VIDEO_YEAR, I18nMarker.marktr("Year"));
            displayNameByKey.put(VIDEO_LANGUAGE, I18nMarker.marktr("Language"));
            displayNameByKey.put(VIDEO_SUBTITLES, I18nMarker.marktr("Subtitles"));
            displayNameByKey.put(VIDEO_RATING, I18nMarker.marktr("Rating"));
            displayNameByKey.put(VIDEO_COMMENTS, I18nMarker.marktr("Comments"));
            displayNameByKey.put(VIDEO_ACTION, I18nMarker.marktr("Action"));
            displayNameByKey.put(VIDEO_LICENSE, I18nMarker.marktr("Copyright"));
            displayNameByKey.put(VIDEO_LICENSETYPE, I18nMarker.marktr("License Type"));
            displayNameByKey.put(VIDEO_HEIGHT, I18nMarker.marktr("Height"));
            displayNameByKey.put(VIDEO_WIDTH, I18nMarker.marktr("Width"));
            displayNameByKey.put(VIDEO_BITRATE, I18nMarker.marktr("Bitrate"));

            displayNameByKey.put(IMAGE, I18nMarker.marktr("Image"));
            displayNameByKey.put(IMAGE_TITLE, I18nMarker.marktr("Title"));
            displayNameByKey.put(IMAGE_DESCRIPTION, I18nMarker.marktr("Description"));
            displayNameByKey.put(IMAGE_ARTIST, I18nMarker.marktr("Artist"));
            displayNameByKey.put(IMAGE_LICENSE, I18nMarker.marktr("License"));
            displayNameByKey.put(IMAGE_LICENSETYPE, I18nMarker.marktr("License Type"));

            displayNameByKey.put(DOCUMENT, I18nMarker.marktr("Document"));
            displayNameByKey.put(DOCUMENT_TITLE, I18nMarker.marktr("Title"));
            displayNameByKey.put(DOCUMENT_TOPIC, I18nMarker.marktr("Topic"));
            displayNameByKey.put(DOCUMENT_AUTHOR, I18nMarker.marktr("Author"));
            displayNameByKey.put(DOCUMENT_LICENSE, I18nMarker.marktr("License"));
            displayNameByKey.put(DOCUMENT_LICENSETYPE, I18nMarker.marktr("License Type"));

            displayNameByKey.put(APPLICATION, I18nMarker.marktr("Application"));
            displayNameByKey.put(APPLICATION_NAME, I18nMarker.marktr("Name"));
            displayNameByKey.put(APPLICATION_PUBLISHER, I18nMarker.marktr("Publisher"));
            displayNameByKey.put(APPLICATION_PLATFORM, I18nMarker.marktr("Platform"));
            displayNameByKey.put(APPLICATION_LICENSETYPE, I18nMarker.marktr("License Type"));
            displayNameByKey.put(APPLICATION_LICENSE, I18nMarker.marktr("License"));        
        }
        
        return displayNameByKey.get(key);
    }
    
}
