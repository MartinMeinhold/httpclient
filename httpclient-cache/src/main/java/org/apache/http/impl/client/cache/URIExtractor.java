/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.impl.client.cache;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.annotation.Immutable;
import org.apache.http.client.cache.HeaderConstants;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.protocol.HTTP;

/**
 * @since 4.1
 */
@Immutable
class URIExtractor {

    /**
     * For a given {@link HttpHost} and {@link HttpRequest} get a URI from the
     * pair that I can use as an identifier KEY into my HttpCache
     *
     * @param host The host for this request
     * @param req the {@link HttpRequest}
     * @return String the extracted URI
     */
    public String getURI(HttpHost host, HttpRequest req) {
        return String.format("%s%s", host.toString(), req.getRequestLine().getUri());
    }

    protected String getFullHeaderValue(Header[] headers) {
        if (headers == null)
            return "";

        StringBuilder buf = new StringBuilder("");
        boolean first = true;
        for (Header hdr : headers) {
            if (!first) {
                buf.append(", ");
            }
            buf.append(hdr.getValue().trim());
            first = false;

        }
        return buf.toString();
    }

    /**
     * For a given {@link HttpHost} and {@link HttpRequest} if the request has a
     * VARY header - I need to get an additional URI from the pair of host and
     * request so that I can also store the variant into my HttpCache.
     *
     * @param host The host for this request
     * @param req the {@link HttpRequest}
     * @param entry the parent entry used to track the varients
     * @return String the extracted variant URI
     */
    public String getVariantURI(HttpHost host, HttpRequest req, HttpCacheEntry entry) {
        Header[] varyHdrs = entry.getHeaders(HeaderConstants.VARY);
        if (varyHdrs == null || varyHdrs.length == 0) {
            return getURI(host, req);
        }

        List<String> variantHeaderNames = new ArrayList<String>();
        for (Header varyHdr : varyHdrs) {
            for (HeaderElement elt : varyHdr.getElements()) {
                variantHeaderNames.add(elt.getName());
            }
        }
        Collections.sort(variantHeaderNames);

        try {
            StringBuilder buf = new StringBuilder("{");
            boolean first = true;
            for (String headerName : variantHeaderNames) {
                if (!first) {
                    buf.append("&");
                }
                buf.append(URLEncoder.encode(headerName, HTTP.UTF_8));
                buf.append("=");
                buf.append(URLEncoder.encode(getFullHeaderValue(req.getHeaders(headerName)),
                        HTTP.UTF_8));
                first = false;
            }
            buf.append("}");
            buf.append(getURI(host, req));
            return buf.toString();
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("couldn't encode to UTF-8", uee);
        }
    }

}