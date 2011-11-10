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

import java.io.IOException;
import java.util.Map;

import org.apache.tapestry5.internal.services.ResourceCache;
import org.apache.tapestry5.internal.services.StreamableResource;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.services.InvalidationListener;
import org.apache.tapestry5.services.assets.ResourceDependencies;

/**
 * An interceptor for the {@link StreamableResourceSource} service that handles caching of content.
 */
public class SRSCachingInterceptor implements StreamableResourceSource, InvalidationListener
{
    private final StreamableResourceSource delegate;

    private final Map<Resource, StreamableResource> cache = CollectionFactory.newConcurrentMap();

    public SRSCachingInterceptor(ResourceCache tracker, StreamableResourceSource delegate)
    {
        tracker.addInvalidationListener(this);
        this.delegate = delegate;
    }

    @Override
    public StreamableResource getStreamableResource(Resource baseResource, ResourceDependencies dependencies)
            throws IOException
    {
        StreamableResource result = cache.get(baseResource);

        if (result == null)
        {
            result = delegate.getStreamableResource(baseResource, dependencies);
            cache.put(baseResource, result);
        }

        return result;
    }

    public void objectWasInvalidated()
    {
        cache.clear();
    }
}
