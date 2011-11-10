// Copyright 2011 The Apache Software Foundation
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.internal.services.StreamableResource;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.internal.util.URLChangeTracker;
import org.apache.tapestry5.ioc.services.ClasspathURLConverter;
import org.apache.tapestry5.services.assets.ContentTypeAnalyzer;
import org.apache.tapestry5.services.assets.ResourceDependencies;
import org.apache.tapestry5.services.assets.ResourceTransformer;

public class StreamableResourceSourceImpl implements StreamableResourceSource
{
    private final Map<String, ResourceTransformer> configuration;
    private final ContentTypeAnalyzer contentTypeAnalyzer;
    private final URLChangeTracker tracker;

    public StreamableResourceSourceImpl(Map<String, ResourceTransformer> configuration,
            ContentTypeAnalyzer contentTypeAnalyzer, ClasspathURLConverter classpathURLConverter) {
        this.configuration = configuration;
        this.contentTypeAnalyzer = contentTypeAnalyzer;
        this.tracker = new URLChangeTracker(classpathURLConverter, true);
        
    }

    @Override
    public StreamableResource getStreamableResource(Resource baseResource, ResourceDependencies dependencies) throws IOException
    {
        assert baseResource != null;

        URL url = baseResource.toURL();

        if (url == null)
            throw new IOException(String.format("Resource %s does not exist.", baseResource));

        String fileSuffix = TapestryInternalUtilsFrom5_3.toFileSuffix(baseResource.getFile());

        ResourceTransformer rt = configuration.get(fileSuffix);


        InputStream transformed = (rt == null) ? new BufferedInputStream(url.openStream()) : 
                                                 rt.transform(baseResource, dependencies);

        BytestreamCache bytestreamCache = readStream(transformed);

        transformed.close();

        String contentType = contentTypeAnalyzer.getContentType(baseResource);
        
        long lastModified = tracker.add(url);

        return new CustomStreamableResourceImpl(contentType, lastModified, bytestreamCache);
    }

    private BytestreamCache readStream(InputStream stream) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        TapestryInternalUtils.copy(stream, bos);

        stream.close();

        return new BytestreamCache(bos);
    }

}
