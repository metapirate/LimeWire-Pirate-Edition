package org.limewire.core.impl.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

@Singleton
class PropertyDictionaryImpl implements PropertyDictionary {
    private final Provider<LimeXMLSchemaRepository> schemaRepository;
    private List<String> audioGenres;
    private List<String> videoGenres;
    private List<String> videoRatings;
    private List<String> applicationPlatforms;
    
    @Inject
    public PropertyDictionaryImpl(Provider<LimeXMLSchemaRepository> schemaRepository) {
        this.schemaRepository = schemaRepository;
    }
    
    @Override
    public List<String> getAudioGenres() {
        if (audioGenres == null) {
            audioGenres = Collections.unmodifiableList(getValueList("audio", "genre"));
        }
        return audioGenres;
    }
    
    @Override
    public List<String> getVideoRatings() {
        if (videoRatings == null) {
            videoRatings = Collections.unmodifiableList(getValueList("video", "rating"));
        }
        return videoRatings;
    }

    @Override
    public List<String> getVideoGenres() {
        if (videoGenres == null) {
            videoGenres = Collections.unmodifiableList(getValueList("video", "type"));
        }
        return videoGenres;
    }

    @Override
    public List<String> getApplicationPlatforms() {
        if (applicationPlatforms == null) {
            applicationPlatforms = Collections.unmodifiableList(getValueList("application", "platform"));
        }
        return applicationPlatforms;
    }

    private List<String> getValueList(String schemaDescription, String enumerationName) {
        List<String> values = new ArrayList<String>();
        for (LimeXMLSchema schema : schemaRepository.get().getAvailableSchemas()) {
            if (schemaDescription.equals(schema.getDescription())) {
                for(SchemaFieldInfo info : schema.getEnumerationFields()) {
                    String canonicalizedFieldName = info.getCanonicalizedFieldName();
                    if (canonicalizedFieldName != null && canonicalizedFieldName.endsWith("__" + enumerationName + "__")) {
                        for(NameValue<String> nameValue : info.getEnumerationList()) {
                            values.add(nameValue.getName());
                        }
                        // Ensure a blank one always exists.
                        if(!values.contains("")) {
                            values.add(0, "");
                        }
                    }
                }
            }
        }
        Collections.sort(values);
        return values;
    }
}
