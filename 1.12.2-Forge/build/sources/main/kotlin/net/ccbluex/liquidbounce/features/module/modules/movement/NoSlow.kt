/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.network.IPacketF
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.*
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.chat.packet.packets.Packet
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.injection.backend.InventoryPlayerImpl
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl
import net.ccbluex.liquidbounce.injection.backend.unwrap
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.item.ItemSword
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.util.EnumFacing
import java.util.*

//import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerPosition

@ModuleInfo(name = "NoSlow", description = "Cancels slowness effects caused by soulsand and using items.",
        category = ModuleCategory.MOVEMENT)
class NoSlow : Module() {

    // Highly customizable values

    private val msTimer = MSTimer()
    private val modeValue = ListValue("PacketMode", arrayOf("Vanilla","AAC5","Vulcan"), "Vanilla")
    private val blockForwardMultiplier = FloatValue("BlockForwardMultiplier", 1.0F, 0.2F, 1.0F)
    private val blockStrafeMultiplier = FloatValue("BlockStrafeMultiplier", 1.0F, 0.2F, 1.0F)
    private val consumeForwardMultiplier = FloatValue("ConsumeForwardMultiplier", 1.0F, 0.2F, 1.0F)
    private val consumeStrafeMultiplier = FloatValue("ConsumeStrafeMultiplier", 1.0F, 0.2F, 1.0F)
    private val bowForwardMultiplier = FloatValue("BowForwardMultiplier", 1.0F, 0.2F, 1.0F)
    private val bowStrafeMultiplier = FloatValue("BowStrafeMultiplier", 1.0F, 0.2F, 1.0F)
    private val alert1Value = BoolValue("updateAlert1", true)
    // Blocks
    val soulsandValue = BoolValue("Soulsand", true)


//    private val blinkPackets = mutableListOf<Packet<ICPacketPlayerPosition>>()
    private var lastX = 0.0
    private var lastY = 0.0
    private var lastZ = 0.0
    private var lastOnGround = false

    private var fasterDelay = false
    private var placeDelay = 0L
    private val timer = MSTimer()
    private val alertTimer = MSTimer()
    private var packetBuf = LinkedList<IPacketF<INetHandlerPlayServer>>()
    private var nextTemp = false
    private var waitC03 = false
    private var lastBlockingStat = false




    override fun onEnable() {
//        blinkPackets.clear()
        msTimer.reset()
    }

    override fun onDisable() {
        msTimer.reset()
        packetBuf.clear()
        nextTemp = false
        waitC03 = false
//        blinkPackets.forEach {
//            PacketUtils.sendPacketNoEvent(it)
//        }
//        blinkPackets.clear()
    }


    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (alertTimer.hasTimePassed(10000) && alert1Value.get() && (modeValue.equals("Matrix") || modeValue.equals("Vulcan"))) {
            alertTimer.reset()
        }
        val thePlayer = mc.thePlayer ?: return
        val heldItem = thePlayer.heldItem ?: return

        if (!classProvider.isItemSword(heldItem.item) || !MovementUtils.isMoving)
            return

        val aura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
        if (!thePlayer.isBlocking && !aura.blockingStatus)
            return

        if (modeValue.get().toLowerCase() == "aac5") {
            if (event.eventState == EventState.POST && (thePlayer.isUsingItem || thePlayer.isBlocking || aura.blockingStatus)) {
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerBlockPlacement(WBlockPos(-1, -1, -1), 255, mc.thePlayer!!.inventory.getCurrentItemInHand(), 0f, 0f, 0f))
            }
            return
        }

//        if (event.packet.get() && Backend.MINECRAFT_VERSION_MINOR == 8) {
//            when (event.eventState) {
//                EventState.PRE -> {
//                    val digging = classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos(0, 0, 0), classProvider.getEnumFacing(EnumFacingType.DOWN))
//                    mc.netHandler.addToSendQueue(digging)
//                }
//                EventState.POST -> {
//                    val blockPlace = classProvider.createCPacketPlayerBlockPlacement(WBlockPos(-1, -1, -1), 255, mc.thePlayer!!.inventory.getCurrentItemInHand(), 0.0F, 0.0F, 0.0F)
//                    mc.netHandler.addToSendQueue(blockPlace)
//                }
//            }
//        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {

        if(modeValue.equals("Vulcan") && (isBlocking)) {
            if(msTimer.hasTimePassed(230) && nextTemp) {
                nextTemp = false
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM,WBlockPos.Vulcan, classProvider.getEnumFacing(EnumFacingType.DOWN)))
                if(packetBuf.isNotEmpty()) {
                    var canAttack = false
                    for(packet in packetBuf) {
                        if(packet is ICPacketPlayer) {
                            canAttack = true
                        }
                        if(!((packet is ICPacketUseEntity || packet is ICPacketAnimation) && !canAttack)) {
                            PacketUtils.sendPacketNoEvent(packet)
                        }
                    }
                    packetBuf.clear()
                }
            }
            if(!nextTemp) {
                lastBlockingStat = isBlocking
                if (!isBlocking) {
                    return
                }
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerBlockPlacement(WBlockPos(-1,-1,-1),255, mc.thePlayer!!.inventory.getCurrentItemInHand(),0f,0f,0f))
                nextTemp = true
                waitC03 = modeValue.equals("Vulcan")
                msTimer.reset()
            }
        }
    }

    private val isBlocking: Boolean
        get() = (mc.thePlayer!!.isUsingItem || mc.thePlayer!!.isBlocking) && mc.thePlayer!!.heldItem != null && mc.thePlayer!!.heldItem!!.item is ItemSword

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if((modeValue.equals("Matrix") || modeValue.equals("Vulcan")) && nextTemp) {
            if((packet is ICPacketPlayerDigging || packet is ICPacketPlayerBlockPlacement) && isBlocking) {
                event.cancelEvent()
            }else if (packet is ICPacketPlayer || packet is ICPacketAnimation || packet is ICPacketEntityAction || packet is ICPacketUseEntity || packet is ICPacketPlayerDigging || packet is ICPacketPlayerBlockPlacement) {
                if (modeValue.equals("Vulcan") && waitC03 && packet is ICPacketPlayer) {
                    waitC03 = false
                    return
                }
                packetBuf.add(packet as IPacketF<INetHandlerPlayServer>)
                event.cancelEvent()
            }
        }
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        val heldItem = mc.thePlayer!!.heldItem?.item

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    private fun getMultiplier(item: IItem?, isForward: Boolean): Float {
        return when {
            classProvider.isItemFood(item) || classProvider.isItemPotion(item) || classProvider.isItemBucketMilk(item) -> {
                if (isForward) this.consumeForwardMultiplier.get() else this.consumeStrafeMultiplier.get()
            }
            classProvider.isItemSword(item) -> {
                if (isForward) this.blockForwardMultiplier.get() else this.blockStrafeMultiplier.get()
            }
            classProvider.isItemBow(item) -> {
                if (isForward) this.bowForwardMultiplier.get() else this.bowStrafeMultiplier.get()
            }
            else -> 0.2F
        }
    }
    override val tag: String?
        get() = modeValue.get()

}
