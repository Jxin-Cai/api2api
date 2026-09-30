package com.api2api.infr.protocol;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads Bedrock binary event-stream frames independently of protocol conversion. */
final class BedrockEventStreamReader {

    private static final int EVENT_STREAM_OVERHEAD_BYTES = 16;

    private BedrockEventStreamReader() {
    }

    static BedrockEvent readEvent(InputStream inputStream) throws IOException {
        DataInputStream dataInput = new DataInputStream(inputStream);
        int totalLength;
        try {
            totalLength = dataInput.readInt();
        } catch (EOFException exception) {
            return null;
        }
        int headersLength = dataInput.readInt();
        dataInput.readInt(); // prelude CRC
        if (totalLength < EVENT_STREAM_OVERHEAD_BYTES || headersLength < 0) {
            throw new IOException("Invalid Bedrock event-stream frame length");
        }
        byte[] headers = dataInput.readNBytes(headersLength);
        int payloadLength = totalLength - EVENT_STREAM_OVERHEAD_BYTES - headersLength;
        if (payloadLength < 0) {
            throw new IOException("Invalid Bedrock event-stream payload length");
        }
        byte[] payload = dataInput.readNBytes(payloadLength);
        dataInput.readInt(); // message CRC
        BedrockEventHeaders eventHeaders = parseHeaders(headers);
        return new BedrockEvent(
                eventHeaders.eventType(),
                eventHeaders.messageType(),
                eventHeaders.exceptionType(),
                payload
        );
    }

    private static BedrockEventHeaders parseHeaders(byte[] headers) throws IOException {
        DataInputStream input = new DataInputStream(new java.io.ByteArrayInputStream(headers));
        String eventType = "";
        String messageType = "";
        String exceptionType = "";
        while (input.available() > 0) {
            int nameLength = input.readUnsignedByte();
            String name = new String(input.readNBytes(nameLength), StandardCharsets.UTF_8);
            int type = input.readUnsignedByte();
            String value = readHeaderValue(input, type);
            switch (name) {
                case ":event-type" -> eventType = value;
                case ":message-type" -> messageType = value;
                case ":exception-type" -> exceptionType = value;
                default -> {
                }
            }
        }
        return new BedrockEventHeaders(eventType, messageType, exceptionType);
    }

    private static String readHeaderValue(DataInputStream input, int type) throws IOException {
        return switch (type) {
            case 0, 1 -> "";
            case 2 -> {
                input.readByte();
                yield "";
            }
            case 3 -> {
                input.readShort();
                yield "";
            }
            case 4 -> {
                input.readInt();
                yield "";
            }
            case 5, 8 -> {
                input.readLong();
                yield "";
            }
            case 6 -> {
                int length = input.readUnsignedShort();
                input.readNBytes(length);
                yield "";
            }
            case 7 -> {
                int length = input.readUnsignedShort();
                yield new String(input.readNBytes(length), StandardCharsets.UTF_8);
            }
            case 9 -> {
                input.readNBytes(16);
                yield "";
            }
            default -> throw new IOException("Unsupported Bedrock event-stream header type: " + type);
        };
    }

    private record BedrockEventHeaders(String eventType, String messageType, String exceptionType) {
    }

    static record BedrockEvent(String eventType, String messageType, String exceptionType, byte[] payload) {
    }

}
