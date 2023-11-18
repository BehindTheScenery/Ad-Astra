package earth.terrarium.ad_astra.mixin;

import earth.terrarium.ad_astra.common.config.AdAstraConfig;
import earth.terrarium.ad_astra.common.util.ModUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract CompoundTag saveWithoutId(CompoundTag compound);

    @Shadow public abstract void kill();

    @Inject(method = "tick", at = @At("TAIL"))
    private void ad_astra$tick(CallbackInfo ci) {
        Entity entity = ((Entity) (Object) this);

        // Teleport the entity to the planet when they fall in the void while in an orbit dimension
        if (entity.getY() < entity.level.getMinBuildHeight() && ModUtils.isOrbitlevel(entity.level)) {
            switch (AdAstraConfig.actionOnFallFromOrbit) {
                case KILL: {
                    this.kill();
                    break;
                }
                case TELEPORT_UP:{
                    entity.teleportTo(entity.getX(), entity.level.getMaxBuildHeight() + 64, entity.getZ());
                    break;
                }
                case TELEPORT_TO_PLANET:
                default:
                    ModUtils.teleportToLevel(ModUtils.getPlanetOrbit(entity.level), entity);
                    break;
            }
        }
    }
}