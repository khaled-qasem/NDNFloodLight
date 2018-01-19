NDN Floodlight OpenFlow Controller (OSS)
====================================

Build Status:
-------------

[![Build Status](https://travis-ci.org/floodlight/floodlight.svg?branch=master)](https://travis-ci.org/floodlight/floodlight)

Floodlight Wiki
---------------

First, the Floodlight wiki has moved. Please check this :

https://floodlight.atlassian.net/wiki/display/floodlightcontroller/Floodlight+Documentation

What is Floodlight?
-------------------

Floodlight is the leading open source SDN controller. It is supported by a community of developers including a number of engineers from Big Switch Networks (http://www.bigswitch.com/).

OpenFlow is a open standard managed by Open Networking Foundation. It specifies a protocol through switch a remote controller can modify the behavior of networking devices through a well-defined “forwarding instruction set”. Floodlight is designed to work with the growing number of switches, routers, virtual switches, and access points that support the OpenFlow standard.


What is NDN Floodlight?
-------------------

Named Data Networking over Internet Protocol Network is a project implementing a new in-network caching idea. An automatic in-network caching mechanism that solves the problem of current cashing techniques (ex. Proxy) which require configuration of each user's browser, which is a costly and un-scalable management task for service providers and large enterprises.
By this we will reduce the overhead on the network, offer a better bandwidth sharing experience and offer a faster responses for previously cached items.

Our idea was implemented in ip network as a software base forwarding schema in OpenFlow –controlled Software – Defined Networks (SDNs). It consists of SDN controller module, Cache Simulator like content store in NDN and a JAVA client/server app used as a proof of concept.
Java based OpenFlow controller (Floodlight) was used to implement the controller module. Core java and open source JNetPcap java library was used to implement an in-network cache and java client/server application.

So we built a java client app which is a simple browser that takes a URL containing the content ID and creates a new packet called “CR” then encapsulates it in an ip packet to be sent. When this packet passes through OVSwitch, it will duplicate the packet and send it to the cache. By its turn, the cache will count these packets by its ID’s and if an ID counter reaches 5 the cache will get the content from original content server and send a command to the controller informing it that it has this content. The controller will send a command to all OVSwitches connected to it in order to inform the switches that the cache has this content. If a client requests the cached content, the OVSwitch will forward this request to cache.

At testing phase, mininet was used with scapy tool to generate packets in mininet hosts.




Floodlight v1.2 can be found on GitHub at:  
http://github.com/floodlight/floodlight/tree/v1.2
