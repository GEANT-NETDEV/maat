package org.geant.maat.notification;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

class BaseListenerCodecProvider implements CodecProvider {
    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry codecRegistry) {
        if (clazz == BaseListener.class) {
            return (Codec<T>) new BaseListenerCodec();
        }
        return null;
    }
}