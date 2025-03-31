# local-powerstream
Knowledge gathering hub with the goal to control the ecoflow powerstream locally without internet

# History
If you arrived here, I probably do not need to come up with reasons why you would want to be able to control a device's basic functionality without asking the manufacturer if you are allowed to do so. But I will tell you a bit my story as background info.
I found the powerstream in my search for a DIY home battery solution (that started by reading this Dutch forum thread: https://gathering.tweakers.net/forum/list_messages/2253584/0) that met all the criteria:
1) the solar inputs can handle the 24V battery as input
2) 2) the power output can be set from 0 to 800W through automation.
3) The device is certified (by our local regulator) to be plugged in a socket in the house.
But there is one big problem left to solve: setting the powerstreams output power without going through a cloud API call. Every IOT or smart device in my home connects to a separate network without internet access, that is simply how my setup is. (my phone has constant VPN to my home server and that server has access to the devices)

# Asking ecoflow nicely will not work
I mailed support with my concerns, mostly because I wanted to see what their response would be. The response was a very general "we see what you ask for and pass it on to our development team". I do not think that not having a local API is caused by the development team. This is 100% a business decision.
Ecoflow is looking into a paid subscription model for functionality that depends on the cloud. The powerstream is relativly cheap (compared to their batteries). They go out of stock in every country. They need to get people hooked on the cloud functionality and want to set up a subscription model later and sell this as "paying for usage of their infrastructure".
Making a local API now would undermine this business strategy.

# The BLE route will not work
The other (portable) Ecoflow products had a bluetooth connection, so the app can be used when out camping. And thus could be controlled locally. Their Powerstream does not have this.

# Tricking the device into connecting to my own mosquitto server
Using adguard I could see that the device is connecting to mqtt-e.ecoflow.com - it was pretty easy to rewrite dns and route this to the IP of my own mosquitto server.
I was going "YES" to see topics appear and all kinds of telemetry from the device being published. This was the most important step, as this meant I could basically get the device talk to my local server.
I noticed the outbound topic and the most logical way the server sets the power output is putting something on a corresponding inbound topic.
The data is in a protobuf binary format, as a next step we need to get this binary format decoded.
<insert issue here to decode the outbound topic>

# Impersonating a device to see what the possible inbound command could be
Using a honeypot-mosquitto server it is possible to log credentials from any connecting client. This worked with my ecoflow river pro.
I could connect with the batteries credentials to ecoflow mqtt, but since you need to use the device specific client id, the way mqtt works is that this throws out the other connection with the same client id.
The device will retry connection and throws you out again. I hope if I can set the power setting in the app right before the disconnect I can briefly see a topic appear, but for I did not succeed.
