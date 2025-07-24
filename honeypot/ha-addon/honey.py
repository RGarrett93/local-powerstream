import socket
import ssl
import logging
import argparse

# Argument parsing
parser = argparse.ArgumentParser(description="MQTT TLS Honeypot")
parser.add_argument("--port", type=int, default=8883, help="Port to listen on")
parser.add_argument("--certfile", type=str, required=True, help="Path to TLS certificate file")
parser.add_argument("--keyfile", type=str, required=True, help="Path to TLS key file")
args = parser.parse_args()

# Logging setup
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger('mqtt_honeypot')

# MQTT CONNACK for v3.1.1
CONNACK_PACKET = b"\x20\x02\x00\x00"

# Varint reader for MQTT remaining length
def read_varint(buf, idx):
    multiplier = 1
    value = 0
    while True:
        encoded = buf[idx]
        value += (encoded & 0x7F) * multiplier
        if (encoded & 0x80) == 0:
            idx += 1
            break
        multiplier *= 128
        idx += 1
    return value, idx

# Extract credentials for MQTT v3.1.1 and v5
def extract_username_password(buf):
    try:
        i = 0
        # fixed header
        if buf[i] != 0x10:
            return None, None
        i += 1
        # remaining length
        remaining, i = read_varint(buf, i)
        # protocol name
        proto_len = (buf[i] << 8) | buf[i+1]
        i += 2 + proto_len
        # protocol level
        level = buf[i]
        i += 1
        # flags
        flags = buf[i]
        has_user = bool(flags & 0x80)
        has_pass = bool(flags & 0x40)
        has_will = bool(flags & 0x04)
        i += 1
        # keep-alive
        i += 2
        # properties v5
        if level == 5:
            _, i = read_varint(buf, i)
        # client id
        cid_len = (buf[i] << 8) | buf[i+1]
        i += 2 + cid_len
        # will
        if has_will:
            if level == 5:
                _, i = read_varint(buf, i)
            will_topic_len = (buf[i] << 8) | buf[i+1]
            i += 2 + will_topic_len
            will_payload_len = (buf[i] << 8) | buf[i+1]
            i += 2 + will_payload_len
        # username
        user = None
        if has_user:
            ulen = (buf[i] << 8) | buf[i+1]
            i += 2
            user = buf[i:i+ulen].decode('utf-8', errors='ignore')
            i += ulen
        # password
        pwd = None
        if has_pass:
            plen = (buf[i] << 8) | buf[i+1]
            i += 2
            pwd = buf[i:i+plen].decode('utf-8', errors='ignore')
        return user, pwd
    except Exception as e:
        logger.error(f"Parsing MQTT failed: {e}")
        return None, None

# Main honeypot loop
ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
ssl_context.load_cert_chain(certfile=args.certfile, keyfile=args.keyfile)

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(("0.0.0.0", args.port))
server.listen(5)
logger.info(f"MQTT TLS Honeypot listening on port {args.port}")

while True:
    client, addr = server.accept()
    logger.info(f"Connection from {addr}")
    try:
        ssock = ssl_context.wrap_socket(client, server_side=True)
        logger.info(f"Handshake successful with {addr}")
        ssock.settimeout(5)
        data = ssock.recv(2048)
        logger.info(f"Raw data (hex): {data.hex()}")
        user, pwd = extract_username_password(data)
        if user or pwd:
            logger.info(f"Captured creds - User: {user}, Pass: {pwd}")
        ssock.sendall(CONNACK_PACKET)
        ssock.close()
    except ssl.SSLError as e:
        logger.error(f"SSL error: {e}")
        client.close()
