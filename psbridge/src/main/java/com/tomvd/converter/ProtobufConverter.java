package com.tomvd.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tomvd.psbridge.Ecoflow;
import com.tomvd.psbridge.HeaderMessage;
import com.tomvd.psbridge.InverterHeartbeat;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProtobufConverter  {
    private static final Logger LOG = LoggerFactory.getLogger(ProtobufConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String convert(byte[] data) throws JsonProcessingException {
        // This is a placeholder implementation
        // Replace with your actual transformation logic
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        LOG.info("Transforming binary data of size: {}", data.length);
        HeaderMessage msgobj = null;
        try {
                msgobj = HeaderMessage.parseFrom(data);
                } catch (InvalidProtocolBufferException e) {
            LOG.error(e.getMessage());
        }

        assert msgobj != null;
        msgobj.getHeaderList().forEach(header -> {
            var cmdFunc = header.getCmdFunc();
            var cmdId = header.getCmdId();
            var src = header.getSrc();

                //LOG.info(header.toString());
                if (cmdId == 1) {
                    try {
                        InverterHeartbeat heartbeat = InverterHeartbeat.parseFrom(header.getPdata());
                        LOG.info(String.valueOf(heartbeat.getLlcTemp()));
                    } catch (InvalidProtocolBufferException ex) {
                        throw new RuntimeException(ex);
                    }
                }
        });

        // Create a simple JSON object
        ObjectNode jsonNode = objectMapper.createObjectNode();

        // For demonstration, we'll just add the binary data length as a value
        // In a real implementation, you would parse and transform the binary data
        jsonNode.put("value", data.length);
        jsonNode.put("last_updated", System.currentTimeMillis());

        // You might want to add additional fields based on your specific requirements
        // For example, if the binary data contains specific measurements or values

        return objectMapper.writeValueAsString(jsonNode);
    }
}
