package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoException;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.mongojack.MongoJsonMappingException;
import org.mongojack.internal.stream.DBDecoderBsonParser;
import org.mongojack.internal.stream.DBEncoderBsonGenerator;

import java.io.IOException;
import java.io.InputStream;

class BaseListenerCodec implements Codec<BaseListener> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public BaseListener decode(BsonReader bsonReader, DecoderContext decoderContext) {
        try (DBDecoderBsonParser parser = new DBDecoderBsonParser(
                new IOContext(new BufferRecycler(), new BaseListenerCodec.EmptyInputStream(), false), 0,
                (AbstractBsonReader) bsonReader, mapper, UuidRepresentation.STANDARD)) {
            ObjectNode on = mapper.reader().readTree(parser);
            if (on.has("_id")) {
                on.put("id", on.get("_id").asText());
                on.remove("_id");
            }
            return new BaseListener(on);
        } catch (IOException e) {
            throw new RuntimeException("IOException encountered while parsing", e);
        }
    }

    @Override
    public void encode(BsonWriter writer, BaseListener listener, EncoderContext encoderContext) {
        var id = listener.getProperty("id");
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Base resource need to have id when saving to db");
        }
        final var on = (ObjectNode) listener.toJson();
        on.put("_id", id.get().asText());
        on.remove("id");
        try (JsonGenerator generator = new DBEncoderBsonGenerator(writer, UuidRepresentation.STANDARD)) {
            mapper.writeTree(generator, on);
        } catch (JsonMappingException e) {
            throw new MongoJsonMappingException(e);
        } catch (IOException e) {
            throw new MongoException("Error writing object out", e);
        }
    }

    @Override
    public Class<BaseListener> getEncoderClass() {
        return BaseListener.class;
    }

    private static class EmptyInputStream extends InputStream {
        @Override
        public int available() {
            return 0;
        }

        public int read() {
            return -1;
        }
    }

}
