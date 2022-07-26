package traben.entity_texture_features.mixin.entity.blockEntity;

import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.entity_texture_features.mixin.accessor.SpriteAccessor;
import traben.entity_texture_features.texture_handlers.ETFManager;
import traben.entity_texture_features.texture_handlers.ETFTexture;

import java.util.UUID;

import static traben.entity_texture_features.ETFClientCommon.ETFConfigData;

@Mixin(BedBlockEntityRenderer.class)
public abstract class MixinBedBlockEntityRenderer extends BlockEntityRenderer<BedBlockEntity> {

    @Shadow @Final private ModelPart field_20813;
    @Shadow @Final private ModelPart field_20814;
    @Shadow @Final private ModelPart[] legs;
    private boolean isAnimatedTexture = false;
    private ETFTexture thisETFTexture = null;
    private ArmorStandEntity etf$bedStandInDummy = null;
    private Identifier etf$textureOfThis = null;
    private VertexConsumerProvider etf$vertexConsumerProviderOfThis = null;

    public MixinBedBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @ModifyArg(method = "method_3558",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"),
            index = 1)
    private VertexConsumer etf$alterTexture(VertexConsumer vertices) {

        if (isAnimatedTexture || !ETFConfigData.enableCustomTextures || !ETFConfigData.enableCustomBlockEntities)
            return vertices;
        thisETFTexture = ETFManager.getETFTexture(etf$textureOfThis, etf$bedStandInDummy, ETFManager.TextureSource.BLOCK_ENTITY);


        //return thisETFTexture.getTextureIdentifier();

        VertexConsumer alteredReturn = etf$vertexConsumerProviderOfThis.getBuffer(RenderLayer.getEntityCutout(thisETFTexture.getTextureIdentifier(etf$bedStandInDummy)));
        return alteredReturn == null ? vertices : alteredReturn;
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BedBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BedBlockEntity;getWorld()Lnet/minecraft/world/World;",
                    shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void etf$getChestTexture(BedBlockEntity bedBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci, SpriteIdentifier spriteIdentifier) {
        isAnimatedTexture = ((SpriteAccessor) spriteIdentifier.getSprite()).callGetFrameCount() != 1;
        if (!isAnimatedTexture) {
            //hopefully works in modded scenarios, assumes the mod dev uses the actual vanilla code process and texture pathing rules
            String nameSpace = spriteIdentifier.getTextureId().getNamespace();
            String texturePath = "textures/" + spriteIdentifier.getTextureId().getPath() + ".png";
            etf$textureOfThis = new Identifier(nameSpace, texturePath);
            etf$vertexConsumerProviderOfThis = vertexConsumerProvider;
            if (ETFConfigData.enableCustomTextures && ETFConfigData.enableCustomBlockEntities) {
                etf$bedStandInDummy = new ArmorStandEntity(EntityType.ARMOR_STAND, MinecraftClient.getInstance().world);
                etf$bedStandInDummy.setPos(bedBlockEntity.getPos().getX(), bedBlockEntity.getPos().getY(), bedBlockEntity.getPos().getZ());
                //chests don't have uuid so set UUID from something repeatable I chose from block pos
                etf$bedStandInDummy.setUuid(UUID.nameUUIDFromBytes((bedBlockEntity.getPos().toString()+bedBlockEntity.getColor().toString()).getBytes()));
            }
        }
    }

    @Inject(method = "method_3558",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
                    shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void etf$applyEmissiveBed(MatrixStack matrix, VertexConsumerProvider vertexConsumerProvider, boolean bl, Direction direction, SpriteIdentifier spriteIdentifier, int light, int overlay, boolean bl2, CallbackInfo ci, VertexConsumer vertexConsumer) {
        //hopefully works in modded scenarios, assumes the mod dev uses the actual vanilla code process and texture pathing rules
        if (!isAnimatedTexture && ETFConfigData.enableEmissiveBlockEntities && (thisETFTexture != null)) {
            thisETFTexture.renderEmissive(matrix, vertexConsumerProvider, this.field_20813, ETFManager.EmissiveRenderModes.blockEntityMode());
            thisETFTexture.renderEmissive(matrix, vertexConsumerProvider, this.field_20814, ETFManager.EmissiveRenderModes.blockEntityMode());
            thisETFTexture.renderEmissive(matrix, vertexConsumerProvider, this.legs[0], ETFManager.EmissiveRenderModes.blockEntityMode());
            thisETFTexture.renderEmissive(matrix, vertexConsumerProvider, this.legs[1], ETFManager.EmissiveRenderModes.blockEntityMode());
            thisETFTexture.renderEmissive(matrix, vertexConsumerProvider, this.legs[2], ETFManager.EmissiveRenderModes.blockEntityMode());
            thisETFTexture.renderEmissive(matrix, vertexConsumerProvider, this.legs[3], ETFManager.EmissiveRenderModes.blockEntityMode());
        }
    }


}


