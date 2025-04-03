import socket
import ssl
import logging

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger('mqtt_honeypot')

# Create SSL context
ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
# Add your certificate and key
ssl_context.load_cert_chain(certfile='/home/user/mosquitto/data/certs/certificate.pem', keyfile='/home/user/mosquitto/data/certs/key.pem')

# Create a socket
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind(('0.0.0.0', 8883))  # MQTT TLS default port
server_socket.listen(5)

logger.info("MQTT TLS Honeypot started on port 8883")

def extract_mqtt_connect_info(data):
    """Extract username and password from MQTT CONNECT packet"""
    if len(data) < 12:
        return None, None
    
    # Check for MQTT CONNECT packet (first byte should be 0x10)
    if data[0] != 0x10:
        return None, None
    
    try:
        # Skip to variable header
        i = 2
        remaining_length = data[1]
        while remaining_length > 0:
            protocol_name_len = (data[i] << 8) | data[i+1]
            i += 2 + protocol_name_len
            
            # Protocol level
            i += 1
            
            # Connect flags
            connect_flags = data[i]
            has_username = bool(connect_flags & 0x80)
            has_password = bool(connect_flags & 0x40)
            i += 1
            
            # Keep-alive
            i += 2
            
            # Client ID
            client_id_len = (data[i] << 8) | data[i+1]
            i += 2
            client_id = data[i:i+client_id_len].decode('utf-8')
            i += client_id_len
            
            username = password = None
            
            # Username
            if has_username:
                username_len = (data[i] << 8) | data[i+1]
                i += 2
                username = data[i:i+username_len].decode('utf-8')
                i += username_len
            
            # Password
            if has_password:
                password_len = (data[i] << 8) | data[i+1]
                i += 2
                password = data[i:i+password_len].decode('utf-8')
            
            return username, password
            
    except Exception as e:
        logger.error(f"Error parsing MQTT packet: {e}")
        return None, None

while True:
    try:
        client_socket, address = server_socket.accept()
        logger.info(f"Connection from {address}")
        
        # Wrap the socket with SSL/TLS
        try:
            ssl_socket = ssl_context.wrap_socket(client_socket, server_side=True)
            logger.info(f"SSL/TLS handshake successful with {address}")
            
            # Receive MQTT CONNECT packet
            data = ssl_socket.recv(1024)
            logger.info(f"data recv{data}")
            
            username, password = extract_mqtt_connect_info(data)
            if username or password:
                logger.info(f"Captured credentials - Username: {username}, Password: {password}")
            
            # Close the connection
            ssl_socket.close()
            
        except ssl.SSLError as e:
            logger.error(f"SSL Error with {address}: {e}")
            client_socket.close()
            
    except Exception as e:
        logger.error(f"Error: {e}")
