package earth.terrarium.ad_astra.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import earth.terrarium.ad_astra.common.block.machine.AbstractMachineBlock;
import earth.terrarium.ad_astra.common.block.machine.entity.EnergizerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


public class EnergizerBlockEntityRenderer implements BlockEntityRenderer<EnergizerBlockEntity> {

    public EnergizerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(EnergizerBlockEntity blockEntity, float tickDelta, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = blockEntity.getItem(0);

        if (stack.isEmpty()) {
            return;
        }

        if (!blockEntity.getBlockState().getValue(AbstractMachineBlock.LIT)) {
            return;
        }

        poseStack.pushPose();

        // Calculate the current offset in the y value
        double offset = Math.sin((blockEntity.getLevel().getGameTime() + tickDelta) / 8.0) / 8.0;
        // Move the item
        poseStack.translate(0.5, 1.6 + offset, 0.5);

        // Rotate the item
        poseStack.mulPose(Axis.YP.rotationDegrees((blockEntity.getLevel().getGameTime() + tickDelta) * 4));

        int lightAbove = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above());
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, lightAbove, packedOverlay, poseStack, buffer, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }
}
