// Copyright 2006, 2007, 2008, 2009 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package uk.org.cezary.t52transformers.internal;

import org.apache.tapestry5.internal.event.InvalidationEventHubImpl;
import org.apache.tapestry5.internal.services.ResourceCache;
import org.apache.tapestry5.internal.services.StreamableResource;
import org.apache.tapestry5.ioc.Resource;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.ioc.internal.util.URLChangeTracker;
import org.apache.tapestry5.ioc.services.ClasspathURLConverter;
import org.apache.tapestry5.services.ResourceDigestGenerator;
import org.apache.tapestry5.services.UpdateListener;
import org.apache.tapestry5.services.UpdateListenerHub;
import org.apache.tapestry5.services.assets.ResourceDependencies;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class CustomResourceCacheImpl extends InvalidationEventHubImpl implements ResourceCache,
        UpdateListener
{
    private final URLChangeTracker tracker;

    private final ResourceDigestGenerator digestGenerator;
    
    private final StreamableResourceSource resourceSource;

    private final Map<Resource, Cached> cache = CollectionFactory.newConcurrentMap();

    final static long MISSING_RESOURCE_TIME_MODIFIED = -1L;

    private final ResourceDependencies dependencies = new ResourceDependencies() {
        @Override
        public void addDependency(Resource dependency) {
            if (dependency == null) {
                return;
            }
            
            final URL dependencyURL = dependency.toURL();
            if (dependencyURL != null) {
                tracker.add(dependencyURL);
            }
        }
    };

    private class Cached
    {
        final boolean requiresDigest;

        final String digest;

        final long timeModified;

        final org.apache.tapestry5.internal.services.StreamableResource streamable;

        Cached(Resource resource)
        {
            requiresDigest = digestGenerator.requiresDigest(resource.getPath());

            URL url = resource.toURL();

            // The url may be null when a request for a protected asset arrives, because the
            // Resource initially is for the file with the digest incorporated into the path, which
            // means
            // no underlying file exists. Subsequently, we'll strip out the digest and resolve
            // to an actual resource.

            digest = (requiresDigest && url != null) ? digestGenerator.generateDigest(url)
                                                     : null;

            timeModified = (url != null) ? tracker.add(url) : MISSING_RESOURCE_TIME_MODIFIED;

            StreamableResource result;
            try {
                result = (url == null) ? null : resourceSource.getStreamableResource(resource, dependencies);
                
            } catch (IOException e) {
                result = null;
            }
            
            streamable = result;
        }

    }

    public CustomResourceCacheImpl(final ResourceDigestGenerator digestGenerator, ClasspathURLConverter classpathURLConverter, 
            StreamableResourceSource resourceSource, UpdateListenerHub updateListenerHub)
    {
        this.digestGenerator = digestGenerator;
        this.resourceSource = resourceSource;
        tracker = new URLChangeTracker(classpathURLConverter, true);
        updateListenerHub.addUpdateListener(this);
    }

    public void checkForUpdates()
    {
        if (tracker.containsChanges())
        {
            cache.clear();
            tracker.clear();

            fireInvalidationEvent();
        }
    }

    private Cached get(Resource resource)
    {
        Cached result = cache.get(resource);

        if (result == null)
        {
            result = new Cached(resource);
            cache.put(resource, result);
        }

        return result;
    }

    public String getDigest(Resource resource)
    {
        return get(resource).digest;
    }

    public long getTimeModified(Resource resource)
    {
        return get(resource).timeModified;
    }

    public boolean requiresDigest(Resource resource)
    {
        return get(resource).requiresDigest;
    }

    public StreamableResource getStreamableResource(Resource resource)
    {
        return get(resource).streamable;
    }

}
