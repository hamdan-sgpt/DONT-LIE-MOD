package com.corazon.corazonmod.client.renderer;

import com.corazon.corazonmod.entity.MoneyPouchEntity;
import com.corazon.corazonmod.init.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MoneyPouchRenderer extends EntityRenderer<MoneyPouchEntity> {
    private final ItemRenderer itemRenderer;

    public MoneyPouchRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(MoneyPouchEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Direction face = entity.getAttachFace();

        switch (face) {
            case DOWN -> {
                // Tucked flat hugging the underside of ceiling/table
                poseStack.translate(0.0D, -0.02D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
            }
            case NORTH, SOUTH, WEST, EAST -> {
                // Flat hugging vertical wall surface (no sticking out shelf effect)
                poseStack.translate(0.0D, 0.0D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));
            }
            default -> {
                // Flat resting on top of floor/table
                poseStack.translate(0.0D, 0.02D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
            }
        }

        poseStack.scale(0.45F, 0.45F, 0.45F);

        // Use Full Bright lighting so item is never rendered pitch black when attached inside gaps/walls/under tables
        int renderLight = LightTexture.FULL_BRIGHT;

        ItemStack stack = new ItemStack(ModItems.MONEY_POUCH.get());
        this.itemRenderer.renderStatic(
            stack, 
            ItemDisplayContext.FIXED, 
            renderLight, 
            OverlayTexture.NO_OVERLAY, 
            poseStack, 
            buffer, 
            entity.level(), 
            entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MoneyPouchEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
