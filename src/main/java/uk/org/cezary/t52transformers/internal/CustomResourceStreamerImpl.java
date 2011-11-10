// Copyright 2006, 2007, 2008, 2009, 2010, 2011 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package uk.org.cezary.t52transformers.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.InternalConstants;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.internal.services.ResourceCache;
import org.apache.tapestry5.internal.services.ResourceStreamer;
import org.apache.tapestry5.internal.services.ServicesMessages;
import org.apache.tapestry5.internal.services.StreamableResource;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.services.Context;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.ResponseCompressionAnalyzer;


// This class is based on org.apache.tapestry5.internal.services.ResourceStreamerImpl from Tapestry 5.2.6 and 5.3.0
// backporting support for modifiable resources (see StreamableResourceSource)
// it can be removed when we start using Tapestry 5.3
public class CustomResourceStreamerImpl implements ResourceStreamer
{
    static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";

    private final Request request;

    private final Response response;

    private final ResourceCache resourceCache;
    
    private final ResponseCompressionAnalyzer analyzer;

    private final Context context;

    private final int compressionCutoff;

    private final boolean productionMode;

    private final Map<String, String> configuration;

    public CustomResourceStreamerImpl(Request request,

    Context context,
            
    ResourceCache resourceCache, 
    
    Response response,

    StreamableResourceSource streamableResourceSource,

    ResponseCompressionAnalyzer analyzer,

    Map<String, String> configuration,


    @Symbol(SymbolConstants.MIN_GZIP_SIZE)
    int compressionCutoff,

    @Symbol(SymbolConstants.PRODUCTION_MODE)
    boolean productionMode)
    {
        this.request = request;
        this.response = response;

        this.context = context;
        this.resourceCache = resourceCache;
        this.analyzer = analyzer;
        this.compressionCutoff = compressionCutoff;
        this.productionMode = productionMode;
        this.configuration = configuration;
    }

    public void streamResource(final Resource resource) throws IOException
    {
        if (!resource.exists())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, ServicesMessages.assetDoesNotExist(resource));
            return;
        }

        
        StreamableResource streamable = resourceCache.getStreamableResource(resource);

        streamResource(resource, streamable);
    }

    public void streamResource(Resource resource, StreamableResource streamable) throws IOException
    {
        long lastModified = streamable.getLastModified();

        long ifModifiedSince = 0;

        try
        {
            ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE_HEADER);
        }
        catch (IllegalArgumentException ex)
        {
            // Simulate the header being missing if it is poorly formatted.

            ifModifiedSince = -1;
        }

        if (ifModifiedSince > 0)
        {
            if (ifModifiedSince >= lastModified)
            {
                response.sendError(HttpServletResponse.SC_NOT_MODIFIED, "");
                return;
            }
        }

        // Prevent the upstream code from compressing when we don't want to.

        response.disableCompression();

        response.setDateHeader("Last-Modified", lastModified);

        if (productionMode)
            response.setDateHeader("Expires", lastModified + InternalConstants.TEN_YEARS);

        String contentType = identifyContentType(resource, streamable);

        boolean compress = analyzer.isGZipSupported() && streamable.getSize(false) >= compressionCutoff
                && analyzer.isCompressable(contentType);

        int contentLength = streamable.getSize(compress);

        if (contentLength >= 0)
            response.setContentLength(contentLength);

        if (compress)
            response.setHeader(InternalConstants.CONTENT_ENCODING_HEADER, InternalConstants.GZIP_CONTENT_ENCODING);

        InputStream is = null;

        try
        {
            is = streamable.getStream(compress);

            OutputStream os = response.getOutputStream(contentType);

            TapestryInternalUtils.copy(is, os);

            is.close();
            is = null;

            os.close();
        }
        finally
        {
            InternalUtils.close(is);
        }
    }
    
    public String getContentType(Resource resource) throws IOException
    {
        return identifyContentType(resource, resourceCache.getStreamableResource(resource));
    }

    private String identifyContentType(Resource resource, StreamableResource streamble) throws IOException
    {
        String contentType = streamble.getContentType();

        if ("content/unknown".equals(contentType))
            contentType = null;

        if (contentType != null)
            return contentType;

        contentType = context.getMimeType(resource.getPath());

        if (contentType != null)
            return contentType;

        String file = resource.getFile();
        int dotx = file.lastIndexOf('.');

        if (dotx > 0)
        {
            String extension = file.substring(dotx + 1);

            contentType = configuration.get(extension);
        }

        return contentType != null ? contentType : "application/octet-stream";
    }
}
