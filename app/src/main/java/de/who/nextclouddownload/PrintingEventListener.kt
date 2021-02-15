package de.who.nextclouddownload

import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy


class PrintingEventListener : EventListener() {
        var callStartNanos: Long = 0
        private fun printEvent(name: String) {
            val nowNanos = System.nanoTime()
            if (name == "callStart") {
                callStartNanos = nowNanos
            }
            val elapsedNanos = nowNanos - callStartNanos
            System.out.printf("%.3f %s%n", elapsedNanos / 1000000000.0, name)
        }

        override fun callStart(call: Call) {
            printEvent("callStart")
        }

        override fun proxySelectStart(call: Call, url: HttpUrl) {
            printEvent("proxySelectStart")
        }

        override fun proxySelectEnd(
            call: Call,
            url: HttpUrl,
            proxies: List<Proxy>
        ) {
            printEvent("proxySelectEnd")
        }

        override fun dnsStart(call: Call, domainName: String) {
            printEvent("dnsStart")
        }

        override fun dnsEnd(
            call: Call,
            domainName: String,
            inetAddressList: List<InetAddress>
        ) {
            printEvent("dnsEnd")
        }

        override fun connectStart(
            call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy
        ) {
            printEvent("connectStart")
        }

        override fun secureConnectStart(call: Call) {
            printEvent("secureConnectStart")
        }

        override fun secureConnectEnd(
            call: Call,
            handshake: Handshake?
        ) {
            printEvent("secureConnectEnd")
        }

        override fun connectEnd(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?
        ) {
            printEvent("connectEnd")
        }

        override fun connectFailed(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
            ioe: IOException
        ) {
            printEvent("connectFailed")
        }

        override fun connectionAcquired(
            call: Call,
            connection: Connection
        ) {
            printEvent("connectionAcquired")
        }

        override fun connectionReleased(
            call: Call,
            connection: Connection
        ) {
            printEvent("connectionReleased")
        }

        override fun requestHeadersStart(call: Call) {
            printEvent("requestHeadersStart")
        }

        override fun requestHeadersEnd(
            call: Call,
            request: Request
        ) {
            printEvent("requestHeadersEnd")
        }

        override fun requestBodyStart(call: Call) {
            printEvent("requestBodyStart")
        }

        override fun requestBodyEnd(
            call: Call,
            byteCount: Long
        ) {
            printEvent("requestBodyEnd")
        }

        override fun requestFailed(
            call: Call,
            ioe: IOException
        ) {
            printEvent("requestFailed")
        }

        override fun responseHeadersStart(call: Call) {
            printEvent("responseHeadersStart")
        }

        override fun responseHeadersEnd(
            call: Call,
            response: Response
        ) {
            printEvent("responseHeadersEnd")
        }

        override fun responseBodyStart(call: Call) {
            printEvent("responseBodyStart")
        }

        override fun responseBodyEnd(
            call: Call,
            byteCount: Long
        ) {
            printEvent("responseBodyEnd")
        }

        override fun responseFailed(
            call: Call,
            ioe: IOException
        ) {
            printEvent("responseFailed")
        }

        override fun callEnd(call: Call) {
            printEvent("callEnd")
        }

        override fun callFailed(call: Call, ioe: IOException) {
            printEvent("callFailed")
        }

        override fun canceled(call: Call) {
            printEvent("canceled")
        }
    }
