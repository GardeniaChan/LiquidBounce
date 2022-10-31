package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.network.IPacketF
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketKeepAlive
import net.minecraft.network.play.INetHandlerPlayServer
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.*
import net.minecraft.network.Packet


//import net.minecraft.network.play.server.*

object PacketUtils : MinecraftInstance() {
    private val packets = ArrayList<IPacketF<INetHandlerPlayServer>>()


    val ISPacketEntityVelocity.realMotionX: Float
        get() = motionX / 8000f

    val ISPacketEntityVelocity.realMotionY: Float
        get() = motionY / 8000f

    val ISPacketEntityVelocity.realMotionZ: Float
        get() = motionZ / 8000f


    @JvmStatic
    fun sendPacketNoEvent(packet: IPacketF<INetHandlerPlayServer>) {
        packets.add(packet)
        mc.netHandler.addToSendQueue(packet)
    }


}