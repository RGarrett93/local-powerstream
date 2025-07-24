# MQTT TLS Honeypot Add-on

A Home Assistant Supervisor add-on that runs a simple MQTT-over-TLS honeypot on port `8883`. It logs incoming MQTT CONNECT packets over TLS and extracts any embedded username and password credentials.

---

## Prerequisites

* **No other service** (e.g. Mosquitto broker) listening on port `8883`. Stop or disable any existing MQTT broker on that port before using this add-on.
* **TLS Certificate & Key** files:

  * `certificate.pem`
  * `key.pem`
    Place them in your `/ssl` folder before starting the add-on.

Instructions for creating your own TLS Certificate & Key can be found here:

https://github.com/tomvd/local-powerstream/blob/main/mqttserver/mosquitto/data/certs/README.md
 
---

## Installation

1. In Home Assistant: **Supervisor → Add-on Store → ⋮ (top-right) → Repositories → Copy link `Local Repository`**, and then click ADD.
2. Find **MQTT TLS Honeypot** in the Add-on Store and **Install**.
3. In the **Configuration** tab, upload your `certificate.pem` and `key.pem` under the **SSL** section.
4. Start the add-on.

---

## Configuration Options

| Option      | Default               | Description                                    |
| ----------- | --------------------- | ---------------------------------------------- |
| `port`      | `8883`                | TCP port for incoming MQTT TLS connections.    |
| `cert_path` | `ssl/certificate.pem` | Path to the TLS certificate inside the add-on. |
| `key_path`  | `ssl/key.pem`         | Path to the TLS key inside the add-on.         |

These can be modified in Supervisor **Add-on → Configuration**.

---

## Usage

1. Configure your devices or network to point to your Home Assistant host on port `8883`. For example, use a DNS redirect (AdGuard, Pi-hole) or modify device MQTT settings.
2. The add-on will log all incoming TLS handshakes and MQTT CONNECT packets.
3. Captured credentials (if present) appear in the Supervisor add-on logs.

> **Warning:** Ensure your primary MQTT broker is not running on port 8883, or the honeypot will fail to bind.

---

## Example Log Entry

```text
2025-07-16 14:02:10 INFO (MainThread) mqtt_honeypot - Connection from ('192.168.1.100', 52345)
2025-07-16 14:02:10 INFO (MainThread) mqtt_honeypot - SSL/TLS handshake successful with ('192.168.1.100', 52345)
2025-07-16 14:02:10 INFO (MainThread) mqtt_honeypot - Raw data (hex): 10...000
2025-07-16 14:02:10 INFO (MainThread) mqtt_honeypot - Captured credentials - Username: device-XX, Password: abc123
2025-07-16 14:02:10 INFO (MainThread) mqtt_honeypot - Sent CONNACK to ('192.168.1.100', 52345)
```
