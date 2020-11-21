package com.example.androidbridge

import com.biosentry.androidbridge.communication.IJSONTranceiver
import com.biosentry.androidbridge.communication.Point
import com.biosentry.androidbridge.communication.PublishMessage
import com.biosentry.androidbridge.serialization.GsonSerializer
import com.biosentry.androidbridge.serialization.IBridgeMessageSerializer
import com.example.androidbridge.mocks.JsonTranceiverMock
import org.junit.Test
import org.junit.Assert.*

class SerializerUnitTests {

    @Test
    fun serializer_deserialize_publish_message()
    {
        val mockTranceiver = JsonTranceiverMock()

        val serializer : IBridgeMessageSerializer = GsonSerializer()
        val expected = PublishMessage(topic = "/geometry_msgs/Point", msg = Point(
            0.0,
            1.0,
            2.0
        ))

        mockTranceiver.send( serializer.toJson(expected) )
        val actual = serializer.fromJson(mockTranceiver.recv())

        assert( actual is PublishMessage)
        if(actual is PublishMessage) {

            // Structural equality does not work at the class level.
            // Memberwise equality checks have to be done here. Maybe due to polymorphism.
            assert(expected.op == actual.op)
            assert(expected.topic == actual.topic)
            assert(expected.msg == actual.msg)
        }
    }

    @Test
    fun serializer_deserialize_publish_with_prefix()
    {
        val mockTranceiver = JsonTranceiverMock()

        val serializer : IBridgeMessageSerializer = GsonSerializer()
        val expected = PublishMessage(topic = "android/drone/geometry_msgs/Point", msg = Point(
            0.0,
            1.0,
            2.0
        ))

        mockTranceiver.send( serializer.toJson(expected) )
        val actual = serializer.fromJson(mockTranceiver.recv())

        assert( actual is PublishMessage)
        if(actual is PublishMessage)
        {
            // Structural equality does not work at the class level.
            // Memberwise equality checks have to be done here. Maybe due to polymorphism.

            println(actual.topic)
            assert(expected.op == actual.op)
            assert(expected.topic == actual.topic)
            assert(expected.msg == actual.msg)
        }
    }
}