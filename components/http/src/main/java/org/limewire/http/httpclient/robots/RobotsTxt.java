/* Robots.java
 *
 * Created Sep 1, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

/**
 * Utility class for parsing and representing 'robots.txt' format 
 * directives, into a list of named user-agents and map from user-agents 
 * to RobotsDirectives. 
 * <p>
 * Based on Robotstxt from Heritrix the web crawler from 
 * http://crawler.archive.org/
 */
public class RobotsTxt {
    
    private static final Log LOG = LogFactory.getLog(RobotsTxt.class);
        
    // all user agents contained in this robots.txt
    // may be thinned of irrelevant entries
    private final List<String> userAgents = new ArrayList<String>(5);
    // map user-agents to directives
    private final Map<String,RobotsDirectives> agentsToDirectives = 
        new HashMap<String,RobotsDirectives>();

    private final static RobotsDirectives NO_DIRECTIVES = new RobotsDirectives();
    
    public RobotsTxt(String robotsTxt) throws InvalidDataException {
        // current is the disallowed paths for the preceding User-Agent(s)
        RobotsDirectives current = null;
        // whether a non-'User-Agent' directive has been encountered
        boolean hasDirectivesYet = false; 
        String catchall = null;
        
        for (String line : robotsTxt.split("\n")) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            if (line.trim().startsWith("#")) {
                LOG.debugf("skipping comment line {0}", line);
                continue;
            }
            // remove any html markup
            line = line.replaceAll("<[^>]+>","");
            int commentIndex = line.indexOf("#");
            if (commentIndex > -1) {
                // Strip trailing comment
                line = line.substring(0, commentIndex);
            }
            line = line.trim();
            if (line.matches("(?i)^User-agent:.*")) {
                String ua = line.substring(11).trim().toLowerCase(Locale.US);
                if (current == null || hasDirectivesYet ) {
                    // only create new rules-list if necessary
                    // otherwise share with previous user-agent
                    current = new RobotsDirectives();
                    hasDirectivesYet = false; 
                }
                if (ua.equals("*")) {
                    ua = "";
                    catchall = ua;
                } else {
                    userAgents.add(ua);
                }
                agentsToDirectives.put(ua, current);
            } else if (line.matches("(?i)Disallow:.*")) {
                if (current == null) {
                    throw new InvalidDataException();
                }
                String path = line.substring(9).trim();
                current.addDisallow(path);
                hasDirectivesYet = true; 
            } else if (line.matches("(?i)Crawl-delay:.*")) {
                if (current == null) {
                    throw new InvalidDataException();
                }
                // consider a crawl-delay, even though we don't 
                // yet understand it, as sufficient to end a 
                // grouping of User-Agent lines
                hasDirectivesYet = true;
                String val = line.substring(12).trim();
                val = val.split("[^\\d\\.]+")[0];
                try {
                    current.setCrawlDelay(Float.parseFloat(val));
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            } else if (line.matches("(?i)Allow:.*")) {
                if (current == null) {
                    throw new InvalidDataException();
                }
                String path = line.substring(6).trim();
                current.addAllow(path);
                hasDirectivesYet = true;
            } else {
                LOG.debugf("unknown line {0}", line);
            }
        }
        
        if (catchall != null) {
            userAgents.add(catchall);
        }
    }

    public List<String> getUserAgents() {
        return userAgents;
    }

    public RobotsDirectives getDirectivesFor(String ua) {
        // find matching ua
        for(String uaListed : userAgents) {
            if (ua.contains(uaListed)) {
                return agentsToDirectives.get(uaListed);
            }
        }
        // no applicable user-agents, so empty directives
        return NO_DIRECTIVES; 
    }
}
