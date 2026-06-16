package com.github.entityinfo.modules;

import com.github.entityinfo.mixin.AccessorServerboundInteractPacket;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.SettingGroup;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.network.PacketUtils;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;

@Environment(EnvType.CLIENT)
public final class CriticalsPlus extends Module {

    public static final CriticalsPlus INSTANCE = new CriticalsPlus();

    private final SettingGroup sgGeneral = settingGroup("General");

    private final BoolSetting onlyGround = boolSetting("Only Ground", true).group(sgGeneral);
    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.OldNCP).group(sgGeneral);

    private CriticalsPlus() {
        super("Criticals+", Category.COMBAT);
    }

    @Override
    public String getInfo() {
        return mode.getValue().name();
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (nullCheck() || mc.player == null || mc.level == null) {
            return;
        }

        if (!(event.getPacket() instanceof ServerboundInteractPacket packet)) {
            return;
        }

        if (!isAttackPacket(packet)) {
            return;
        }

        Entity target = mc.level.getEntity(((AccessorServerboundInteractPacket) (Object) packet).entityinfo$getEntityId());
        if (!canCrit(target)) {
            return;
        }

        mc.player.crit(target);
        doCrit();
    }

    private boolean canCrit(Entity target) {
        if (target == null || target instanceof EndCrystal) {
            return false;
        }

        if (mc.player.isPassenger() || mc.player.isInWater() || mc.player.onClimbable()) {
            return false;
        }

        if (onlyGround.getValue() && !mc.player.onGround() && !mc.player.horizontalCollision) {
            return false;
        }

        return true;
    }

    private boolean isAttackPacket(ServerboundInteractPacket packet) {
        Object action = ((AccessorServerboundInteractPacket) (Object) packet).entityinfo$getAction();
        if (action == null) {
            return false;
        }

        try {
            Method getType = action.getClass().getDeclaredMethod("getType");
            getType.setAccessible(true);
            Object actionType = getType.invoke(action);
            return actionType != null && "ATTACK".equals(actionType.toString());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void doCrit() {
        switch (mode.getValue()) {
            case Strict -> {
                if (mc.level.getBlockState(mc.player.blockPosition()).getBlock() == Blocks.COBWEB) {
                    return;
                }
                sendPos(mc.player.getX(), mc.player.getY() + 0.062600301692775D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 0.07260029960661D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false);
            }
            case NCP -> {
                sendPos(mc.player.getX(), mc.player.getY() + 0.0625D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false);
            }
            case OldNCP -> {
                sendPos(mc.player.getX(), mc.player.getY() + 1.058293536E-5D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 9.16580235E-6D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 1.0371854E-7D, mc.player.getZ(), false);
            }
            case NewNCP -> {
                sendPos(mc.player.getX(), mc.player.getY() + 2.71875E-7D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false);
            }
            case Hypixel2K22 -> {
                sendPos(mc.player.getX(), mc.player.getY() + 0.0045D, mc.player.getZ(), true);
                sendPos(mc.player.getX(), mc.player.getY() + 1.52121E-4D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 0.3D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 0.025D, mc.player.getZ(), false);
            }
            case Packet -> {
                sendPos(mc.player.getX(), mc.player.getY() + 5.0E-4D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 1.0E-4D, mc.player.getZ(), false);
            }
            case BBTT -> {
                if (mc.player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D) {
                    return;
                }
                sendPos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true);
                sendPos(mc.player.getX(), mc.player.getY() + 0.0625D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 0.045D, mc.player.getZ(), false);
            }
            case LowPacket -> {
                PacketUtils.sendSilently(new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(),
                    mc.player.getY() + 2.71875E-7D,
                    mc.player.getZ(),
                    false,
                    false
                ));
                PacketUtils.sendSilently(new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    false,
                    false
                ));
            }
            case Grim -> {
                sendPos(mc.player.getX(), mc.player.getY() + 0.0625D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 0.04535D, mc.player.getZ(), false);
            }
            case GrimV2 -> PacketUtils.sendSilently(new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(),
                mc.player.getY() - 1.0E-4D,
                mc.player.getZ(),
                mc.player.getYRot(),
                mc.player.getXRot(),
                false,
                false
            ));
            case GrimCC -> {
                sendPos(mc.player.getX(), mc.player.getY() + 0.0625D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 0.0625013579D, mc.player.getZ(), false);
                sendPos(mc.player.getX(), mc.player.getY() + 1.3579E-6D, mc.player.getZ(), false);
            }
            case New2b2t, GrimV3 -> {
                PacketUtils.sendSilently(new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(),
                    mc.player.getY() + 2.71875E-7D,
                    mc.player.getZ(),
                    false,
                    false
                ));
                PacketUtils.sendSilently(new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    false,
                    false
                ));
            }
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        PacketUtils.sendSilently(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, false));
    }

    @Environment(EnvType.CLIENT)
    private enum Mode {
        NewNCP,
        Strict,
        NCP,
        OldNCP,
        Hypixel2K22,
        Packet,
        BBTT,
        LowPacket,
        GrimCC,
        GrimV2,
        Grim,
        New2b2t,
        GrimV3
    }
}
