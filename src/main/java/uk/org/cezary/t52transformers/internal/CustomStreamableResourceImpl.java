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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.internal.services.StreamableResource;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;

public class CustomStreamableResourceImpl implements StreamableResource
{
    private final String contentType;

    private final long lastModified;

    private final BytestreamCache bytestreamCache;
    private BytestreamCache compressedBytestreamCache;
    
    public CustomStreamableResourceImpl(String contentType, long lastModified,
            BytestreamCache bytestreamCache)
    {
        this.contentType = contentType;
        this.lastModified = lastModified;
        this.bytestreamCache = bytestreamCache;
    }

    public String getContentType()
    {
        return contentType;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public void streamTo(OutputStream os) throws IOException
    {
        bytestreamCache.writeTo(os);
    }

    @Override
    public String toString()
    {
        int size = 0;
        try {
            size = getSize(false);
        } catch (IOException e) {
        }
        
        return String.format("StreamableResource<%s lastModified: %tc size: %d>", contentType, 
                lastModified, size);
    }

    @Override
    public int getSize(boolean compress) throws IOException {
        if (compress) {
            updateCompressedCachedValues();
            return this.compressedBytestreamCache.size();
        }
        return this.bytestreamCache.size();
    }

    @Override
    public InputStream getStream(boolean compress) throws IOException {
        if (compress) {
            updateCompressedCachedValues();
            return this.compressedBytestreamCache.openStream();
        }
        return this.bytestreamCache.openStream();
    }

    private void updateCompressedCachedValues() throws IOException
    {
        if (compressedBytestreamCache != null) return;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream compressor = new BufferedOutputStream(new GZIPOutputStream(bos));

        InputStream is = null;

        try
        {
            is = bytestreamCache.openStream();

            TapestryInternalUtils.copy(is, compressor);

            compressor.close();
        }
        finally
        {
            InternalUtils.close(is);
        }

        this.compressedBytestreamCache = new BytestreamCache(bos.toByteArray());
    }
}
