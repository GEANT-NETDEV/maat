package org.geant.maat.resource;


import org.geant.maat.resource.dto.BaseResource;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

class BaseResourceCodecProvider implements CodecProvider {
    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry codecRegistry) {
        if (clazz == BaseResource.class) {
            return (Codec<T>) new BaseResourceCodec();
        }
        return null;
    }
}