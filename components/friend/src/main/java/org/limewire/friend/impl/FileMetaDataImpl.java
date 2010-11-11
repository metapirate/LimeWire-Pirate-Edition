package org.limewire.friend.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.friend.api.FileMetaData;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

public class FileMetaDataImpl implements FileMetaData {

    /**
     * Keep casing of enum names since they are being sent over the wire
     * and xml is case sensitive.
     */
    public static enum Element {
        id, name, size, description, index, metadata, urns, createTime
    }

    private static final Element[] MANDATORY_FIELDS = new Element[] {
        Element.index, Element.name, Element.size, Element.createTime, Element.urns
    };
    
    protected final Map<String, String> data = new HashMap<String, String>();

    public FileMetaDataImpl() {}
    
    /**
     * 
     * @throws IllegalArgumentException if data is not complete
     */
    public FileMetaDataImpl(FileMetaData metaData) {
        setCreateTime(metaData.getCreateTime());
        setDescription(metaData.getDescription());
        setId(metaData.getId());
        setIndex(metaData.getIndex());
        setName(metaData.getName());
        setSize(metaData.getSize());
        setURNs(metaData.getUrns());
        if (!isValid()) {
            throw new IllegalArgumentException("is missing mandatory fields: " + this);
        }
    }
    
    /**
     * 
     * @throws IllegalArgumentException if data is not complete
     */
    public FileMetaDataImpl(Map<Element, String> data) {
        for (Entry<Element, String> entry : data.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        if (!isValid()) {
            throw new IllegalArgumentException("is missing mandatory fields: " + this);
        }
    }
    
    protected boolean isValid() {
        for (Element element : MANDATORY_FIELDS) {
            if (get(element) == null) {
                return false;
            }
        }
        try {
            getSize();
            getIndex();
            getCreateTime();
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    
    private void put(Element element, String value) {
        data.put(element.toString(), value);
    }
    
    private String get(Element element) {
        return data.get(element.toString());
    }

    public String getId() {
        return get(Element.id);
    }
    
    public void setId(String id) {
        put(Element.id, id);
    }

    public String getName() {
        return get(Element.name);
    }
    
    public void setName(String name) {
        put(Element.name, Objects.nonNull(name, "name"));
    }

    public long getSize() {
        return Long.valueOf(get(Element.size));
    }                                                     
    
    public void setSize(long size) {
        put(Element.size, Long.toString(size));
    }

    public String getDescription() {
        return get(Element.description);
    }
    
    public void setDescription(String description) {
        put(Element.description, description);
    }

    public long getIndex() {
        return Long.valueOf(get(Element.index));
    }
    
    public void setIndex(long index) {
        put(Element.index, Long.toString(index));
    }

    public Set<String> getUrns() {
        StringTokenizer st = new StringTokenizer(get(Element.urns), " ");
        Set<String> set = new HashSet<String>();
        while(st.hasMoreElements()) {
            set.add(st.nextToken());
        }
        return set;
    }

    public void setURNs(Set<String> urns) {
        put(Element.urns, StringUtils.explode(urns, " "));
    }

    public Date getCreateTime() {
        return new Date(Long.valueOf(get(Element.createTime)));
    }
    
    public void setCreateTime(Date date) {
        put(Element.createTime, Long.toString(date.getTime()));
    }

    @Override
    public Map<String, String> getSerializableMap() {
        return Collections.unmodifiableMap(data);
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this, data);
    }
}
