/* RobotsDirectives.java
 *
 * Created April 29, 2008
 *
 * Copyright (C) 2008 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.limewire.http.httpclient.robots;

import org.limewire.collection.CharSequenceKeyAnalyzer;
import org.limewire.collection.PatriciaTrie;
import org.limewire.util.StringUtils;

/**
 * Represents the directives that apply to a user-agent (or set of
 * user-agents)
 * <p>
 * Based on RobotsDirectives from Heritrix the web crawler from 
 * http://crawler.archive.org/
 */
public class RobotsDirectives {
        
    private final PatriciaTrie<String, String> disallows = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
    private final PatriciaTrie<String, String> allows = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
    private float crawlDelay = -1; 

    public boolean allows(String path) {
        String closest = disallows.select(path);
        if (closest != null && path.startsWith(closest)) {
            closest = allows.select(path);
            return closest != null && path.startsWith(closest); 
        }
        return true;
    }

    public void addDisallow(String path) {
        if(path.isEmpty()) {
            // ignore empty-string disallows 
            // (they really mean allow, when alone)
            return;
        }
        disallows.put(path, path);
    }

    public void addAllow(String path) {
        allows.put(path, path);
    }

    public void setCrawlDelay(float i) {
        crawlDelay=i;
    }

    public float getCrawlDelay() {
        return crawlDelay;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}