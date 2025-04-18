package com.tomvd.converter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tomvd.psbridge.HeaderMessage;
import com.tomvd.psbridge.InverterHeartbeat;
import com.tomvd.psbridge.SendMsgHart;
import com.tomvd.psbridge.setMessage;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProtobufConverter  {
    private static final Logger LOG = LoggerFactory.getLogger(ProtobufConverter.class);

    public InverterHeartbeat convert(byte[] data) {
        LOG.info("Transforming binary data of size: {}", data.length);
        HeaderMessage msgobj = null;
        try {
                msgobj = HeaderMessage.parseFrom(data);
                } catch (InvalidProtocolBufferException e) {
            LOG.error(e.getMessage());
        }

        assert msgobj != null;
        return msgobj.getHeaderList().stream()
                .filter(header -> header.getCmdId() == 1)
                .map(header -> {
                    try {
                        return InverterHeartbeat.parseFrom(header.getPdata());
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                }).findFirst().orElse(null);
    }

    public byte[] getPowerSettingPayload(int watts, String sn) {
        int deciWatts = Math.max(1, watts*10);
        setMessage setMessage = com.tomvd.psbridge.setMessage.newBuilder()
                .setHeader(com.tomvd.psbridge.setHeader.newBuilder()
                        .setPdata(com.tomvd.psbridge.setValue.newBuilder()
                                .setValue(deciWatts)
                                .build())
                        .setSrc(32)
                        .setDest(53)
                        .setDSrc(1)
                        .setDDest(1)
                        .setCheckType(3)
                        .setCmdFunc(20)
                        .setCmdId(129)
                        .setDataLen(deciWatts > 127?3:2)
                        .setNeedAck(1)
                        .setSeq((int)(System.currentTimeMillis()/1000))
                        .setVersion(19)
                        .setPayloadVer(1)
                        .setFrom("ios")
                        .setDeviceSn(sn)
                        .build())
                .build();
        return setMessage.toByteArray();
    }

    /*
      "link_id": 15,
  "src": 32,
  "dest": 53,
  "d_src": 1,
  "d_dest": 1,
  "enc_type": 0,
  "check_type": 0,
  "cmd_func": 32,
  "cmd_id": 10,
  "data_len": 2,
  "need_ack": 1,
  "is_ack": 0,
  "ack_type": 0,
  "seq": 1065348852,
  "time_snap": 0,
  "is_rw_cmd": 0,
  "is_queue": 0,
  "product_id": 0,
  "version": 0
     */
    /*
    not sure what this message is to be honest. It is sent by the ecoflow mqtt server the moment a device connects to it.
    And it seems to keep the device chatting. Otherwise it falls back to a very slow rate of 48seconds status updates
     */
    public byte[] convertHB() {
        SendMsgHart sendMsgHart = com.tomvd.psbridge.SendMsgHart.newBuilder()
                .setLinkId(15)
                .setSrc(32)
                .setDest(53)
                .setDSrc(1)
                .setDDest(1)
                .setEncType(0)
                .setCheckType(0)
                .setCmdFunc(32)
                .setCmdId(10)
                .setDataLen(2)
                .setNeedAck(1)
                .setIsAck(0)
                .setAckType(0)
                .setSeq((int)(System.currentTimeMillis()/1000))
                .build();
        return sendMsgHart.toByteArray();
    }
}
