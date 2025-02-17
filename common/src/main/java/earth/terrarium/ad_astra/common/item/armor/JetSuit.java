package earth.terrarium.ad_astra.common.item.armor;

import dev.architectury.injectables.annotations.PlatformOnly;
import earth.terrarium.ad_astra.AdAstra;
import earth.terrarium.ad_astra.client.screen.PlayerOverlayScreen;
import earth.terrarium.ad_astra.common.config.SpaceSuitConfig;
import earth.terrarium.ad_astra.common.registry.ModItems;
import earth.terrarium.ad_astra.common.util.ModKeyBindings;
import earth.terrarium.ad_astra.common.util.ModUtils;
import earth.terrarium.botarium.common.energy.base.BotariumEnergyItem;
import earth.terrarium.botarium.common.energy.impl.SimpleEnergyContainer;
import earth.terrarium.botarium.common.energy.impl.WrappedItemEnergyContainer;
import earth.terrarium.botarium.common.energy.util.EnergyHooks;
import earth.terrarium.botarium.common.item.ItemStackHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JetSuit extends NetheriteSpaceSuit implements BotariumEnergyItem<WrappedItemEnergyContainer> {

    private boolean isFallFlying;
    private boolean emitParticles;

    public JetSuit(ArmorMaterial material, ArmorItem.Type type, net.minecraft.world.item.Item.Properties properties) {
        super(material, type, properties);
    }

    public void spawnParticles(Level level, LivingEntity entity, HumanoidModel<LivingEntity> model) {
        if (!SpaceSuitConfig.spawnJetSuitParticles || !emitParticles) return;

        spawnParticles(level, entity, model.rightArm.xRot + 0.05, entity.isFallFlying() ? 0.0 : 0.8, -0.45);
        spawnParticles(level, entity, model.leftArm.xRot + 0.05, entity.isFallFlying() ? 0.0 : 0.8, 0.45);

        spawnParticles(level, entity, model.rightLeg.xRot + 0.05, entity.isFallFlying() ? 0.1 : 0.0, -0.1);
        spawnParticles(level, entity, model.leftLeg.xRot + 0.05, entity.isFallFlying() ? 0.1 : 0.0, 0.1);
    }

    // Spawns particles at the limbs of the player
    private void spawnParticles(Level level, LivingEntity entity, double pitch, double yOffset, double zOffset) {
        double yaw = entity.yBodyRot;
        double xRotator = Math.cos(yaw * Math.PI / 180.0) * zOffset;
        double zRotator = Math.sin(yaw * Math.PI / 180.0) * zOffset;
        double xRotator1 = Math.cos((yaw - 90) * Math.PI / 180.0) * pitch;
        double zRotator1 = Math.sin((yaw - 90) * Math.PI / 180.0) * pitch;

        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, true, entity.getX() + xRotator + xRotator1, entity.getY() + yOffset, entity.getZ() + zRotator1 + zRotator, 0.0, 0.0, 0.0);
    }

    public static boolean hasFullSet(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!(stack.getItem() instanceof JetSuit)) {
                return false;
            }
        }
        return true;
    }

    public static void updateBatteryOverlay(ItemStack suit) {
        var energy = EnergyHooks.getItemEnergyManager(suit);
        PlayerOverlayScreen.batteryRatio = energy.getStoredEnergy() / (double) energy.getCapacity();
    }

    @Override
    public long getTankSize() {
        return SpaceSuitConfig.jetSuitTankSize;
    }

    // Display energy
    @Override
    public void appendHoverText(ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag context) {
        super.appendHoverText(stack, level, tooltip, context);
        if (stack.is(ModItems.JET_SUIT.get())) {
            long energy = EnergyHooks.getItemEnergyManager(stack).getStoredEnergy();
            tooltip.add(Component.translatable("gauge_text.ad_astra.storage", energy, SpaceSuitConfig.jetSuitMaxEnergy).setStyle(Style.EMPTY.withColor(energy > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
    }

    public void fly(Player player, ItemStack stack) {
        emitParticles = false;
        if (!SpaceSuitConfig.enableJetSuitFlight) return;
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
        ItemStackHolder stackHolder = new ItemStackHolder(stack);
        if (player.getAbilities().flying) return;
        if (EnergyHooks.getItemEnergyManager(stack).getStoredEnergy() <= 0) return;

        if (ModKeyBindings.sprintKeyDown(player)) {
            this.fallFly(player, stackHolder);
        } else {
            this.flyUpward(player, stackHolder);
        }

        if (isFallFlying) {
            if (!player.isFallFlying()) {
                player.startFallFlying();
            }
        } else {
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
        }
        emitParticles = true;
        if (stackHolder.isDirty()) player.setItemSlot(EquipmentSlot.CHEST, stackHolder.getStack());
        ModUtils.sendUpdatePacket((ServerPlayer) player);
    }

    public void flyUpward(Player player, ItemStackHolder stack) {
        if (EnergyHooks.isEnergyItem(stack.getStack())) {
            player.fallDistance /= 2;

            var energy = EnergyHooks.getItemEnergyManager(stack.getStack());
            long tickEnergy = SpaceSuitConfig.jetSuitEnergyPerTick;
            if (!player.isCreative()) {
                energy.extract(stack, tickEnergy, false);
                makeSilent(player, stack);
            }
            isFallFlying = false;

            double speed = SpaceSuitConfig.jetSuitUpwardsSpeed;
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, speed, 0.0));
            if (player.getDeltaMovement().y() > speed) {
                player.setDeltaMovement(player.getDeltaMovement().x(), speed, player.getDeltaMovement().z());
            }
        }
    }

    public void fallFly(Player player, ItemStackHolder stack) {
        if (player.onGround()) {
            player.fallDistance /= 2;
        }
        var energy = EnergyHooks.getItemEnergyManager(stack.getStack());
        long tickEnergy = SpaceSuitConfig.jetSuitEnergyPerTick;
        if (!player.isCreative()) {
            energy.extract(stack, tickEnergy, false);
            makeSilent(player, stack);
        }
        isFallFlying = true;

        double speed = SpaceSuitConfig.jetSuitSpeed - (ModUtils.getEntityGravity(player) * 0.25);
        Vec3 rotationVector = player.getLookAngle().scale(speed);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.add(rotationVector.x() * 0.1 + (rotationVector.x() * 1.5 - velocity.x()) * 0.5, rotationVector.y() * 0.1 + (rotationVector.y() * 1.5 - velocity.y()) * 0.5, rotationVector.z() * 0.1 + (rotationVector.z() * 1.5 - velocity.z()) * 0.5));
    }

    private void makeSilent(Player player, ItemStackHolder stack) {
        var wasSilent = player.isSilent();
        if (!wasSilent) player.setSilent(true);
        if (stack.isDirty()) player.setItemSlot(EquipmentSlot.CHEST, stack.getStack());
        if (!wasSilent) player.setSilent(false);
    }

    @Override
    public WrappedItemEnergyContainer getEnergyStorage(ItemStack holder) {
        return new WrappedItemEnergyContainer(holder, new SimpleEnergyContainer(SpaceSuitConfig.jetSuitMaxEnergy) {
            @Override
            public long maxInsert() {
                return 512;
            }

            @Override
            public long maxExtract() {
                return 256;
            }
        });
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (!slot.equals(EquipmentSlot.CHEST)) {
            return new ResourceLocation(AdAstra.MOD_ID, "textures/entity/armour/jet_suit/jet_suit_5.png").toString();
        }
        if (stack.getItem() instanceof JetSuit) {
            var energy = EnergyHooks.getItemEnergyManager(stack);
            return new ResourceLocation(AdAstra.MOD_ID, "textures/entity/armour/jet_suit/jet_suit_" + (energy.getStoredEnergy() <= 0 ? 0 : ((int) Math.min((energy.getStoredEnergy() * 5 / Math.max(1, energy.getCapacity())) + 1, 5))) + ".png").toString();
        }
        return new ResourceLocation(AdAstra.MOD_ID, "textures/entity/armour/jet_suit/jet_suit_5.png").toString();
    }

    public void setFallFlying(boolean fallFlying) {
        isFallFlying = fallFlying;
    }

    public boolean setEmitParticles(boolean emitParticles) {
        return this.emitParticles = emitParticles;
    }

    @PlatformOnly("forge")
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide) {
            ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof JetSuit) {
                int nextFlightTick = flightTicks + 1;
                if (nextFlightTick % 10 == 0) {
                    if (nextFlightTick % 20 == 0) {
                        stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(EquipmentSlot.CHEST));
                    }
                    entity.gameEvent(GameEvent.ELYTRA_GLIDE);
                }
            }
        }
        return true;
    }

    @PlatformOnly("forge")
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return this.isFallFlying;
    }
}