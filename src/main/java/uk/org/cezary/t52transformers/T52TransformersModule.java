// Copyright 2011 Cezary Biernacki, Licensed under the Apache License, Version 2.0 (the "License").
//
// You may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package uk.org.cezary.t52transformers;

import org.apache.tapestry5.internal.services.ResourceCache;
import org.apache.tapestry5.internal.services.ResourceStreamer;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Decorate;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.services.ServiceOverride;
import org.apache.tapestry5.services.TapestryModule;
import org.apache.tapestry5.services.assets.ContentTypeAnalyzer;

import uk.org.cezary.t52transformers.internal.ContentTypeAnalyzerImpl;
import uk.org.cezary.t52transformers.internal.CustomResourceCacheImpl;
import uk.org.cezary.t52transformers.internal.CustomResourceStreamerImpl;
import uk.org.cezary.t52transformers.internal.SRSCachingInterceptor;
import uk.org.cezary.t52transformers.internal.StreamableResourceSource;
import uk.org.cezary.t52transformers.internal.StreamableResourceSourceImpl;


/**
 * Configuration for Tapestry-IOC. Provides capabilities of resource transformation from T5.3 in T5.2.
 * </p>
 * <p>
 * Created: 14 Sep 2011
 * </p>
 * 
 * @author Cezary Biernacki
 */

@SubModule( { TapestryModule.class } )
public class T52TransformersModule {
    
    public static void bind(ServiceBinder binder) {
        binder.bind(ContentTypeAnalyzer.class, ContentTypeAnalyzerImpl.class);
        binder.bind(StreamableResourceSource.class, StreamableResourceSourceImpl.class);
        binder.bind(ResourceCache.class, CustomResourceCacheImpl.class).withId("CustomResourceCacheImpl");
        binder.bind(ResourceStreamer.class, CustomResourceStreamerImpl.class).withId("CustomResourceStreamerImpl");
    }
    
    @Contribute(ServiceOverride.class)
    public static void setupReplacementServices(MappedConfiguration<Class<?>, Object> configuration,
            @Local ResourceCache resourceCache,
            @Local ResourceStreamer resourceStreamer
            ) {
        configuration.add(ResourceCache.class, resourceCache);
        configuration.add(ResourceStreamer.class, resourceStreamer);
    }
    
    @Contribute(ContentTypeAnalyzer.class)
    public static void setupDefaultContentTypeMappings(MappedConfiguration<String, String> configuration)
    {
        configuration.add("css", "text/css");
        configuration.add("js", "text/javascript");
        configuration.add("gif", "image/gif");
        configuration.add("jpg", "image/jpeg");
        configuration.add("jpeg", "image/jpeg");
        configuration.add("png", "image/png");
        configuration.add("swf", "application/x-shockwave-flash");
    }

    @Decorate(id = "Cache", serviceInterface = StreamableResourceSource.class)
    public static StreamableResourceSource enableStreamableCaching(ResourceCache cache, StreamableResourceSource delegate)
    {
        SRSCachingInterceptor interceptor = new SRSCachingInterceptor(cache, delegate);

        cache.addInvalidationListener(interceptor);

        return interceptor;
    }

}
