package org.geant.maat.service;


import org.geant.maat.service.dto.BaseService;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
public class BaseServiceCodecProvider implements CodecProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry codecRegistry) {
        if (clazz == BaseService.class) {
            return (Codec<T>) new BaseServiceCodec();
        }
        return null;
    }
}
