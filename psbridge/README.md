## PSBridge
/!\ **beware this is work in progress, it can not be used just for testing** /!\

The application connects to a local mqtt broker that is set up to receive data from the powerstream.
Optionally you can then connect (with MQTT plugin) home assistant to the same broker.

You can set up your broker to allow anonymous connections (easy but not secure), or you can set it up to use passwords.
When using passwords, make sure you first know the user/name password your ecoflow device uses to connect to mqtt (using the honeypot trick)

For the smart battery functionality, I have set up some home automation tasks to publish certain data to certain topics:
meter-topic: smart/p1
enabled-topic: smart/enabled
charger-topic: smart/charger
soc-topic: smart/soc

p1 is directly power in watts from my p1 meter
soc is the state of charge of my battery (read out via bluetooth)
when "on" is put in the charger topic, the charger goes on
when "off" is put in the charger topic, the charger goes off
