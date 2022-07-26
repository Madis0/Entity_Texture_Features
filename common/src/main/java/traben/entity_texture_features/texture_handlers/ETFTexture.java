package traben.entity_texture_features.texture_handlers;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.ETFClientCommon;
import traben.entity_texture_features.ETFVersionDifferenceHandler;
import traben.entity_texture_features.utils.ETFUtils2;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static traben.entity_texture_features.ETFClientCommon.ETFConfigData;
import static traben.entity_texture_features.texture_handlers.ETFManager.EMISSIVE_SUFFIX_LIST;
import static traben.entity_texture_features.texture_handlers.ETFManager.ENTITY_BLINK_TIME;


//can either refer to a vanilla identifier or a variant
public class ETFTexture {


    //the vanilla texture this is associated with
    //might be itself
    //private final Identifier vanillaIdentifier;

    private final static String PATCH_NAMESPACE_PREFIX = "etf_patched_";
    //this variants id , might be vanilla
    public final Identifier thisIdentifier;
    private final Object2ReferenceOpenHashMap<Identifier, Identifier> FEATURE_TEXTURE_MAP = new Object2ReferenceOpenHashMap<>();
    private final int variantNumber;
    public TextureReturnState currentTextureState = TextureReturnState.NORMAL;
    //a variation of thisIdentifier but with emissive texture pixels removed for z-fighting solution
    private Identifier thisIdentifier_Patched = null;
    //the emissive version of this texture
    private Identifier emissiveIdentifier = null;
    private Identifier emissiveBlinkIdentifier = null;
    private Identifier emissiveBlink2Identifier = null;
    private Identifier blinkIdentifier = null;
    private Identifier blink2Identifier = null;
    private Identifier blinkIdentifier_Patched = null;
    private Identifier blink2Identifier_Patched = null;
    private Integer blinkLength = ETFConfigData.blinkLength;
    private Integer blinkFrequency = ETFConfigData.blinkFrequency;

    // private final TextureSource source;

    public ETFTexture(/*@NotNull Identifier vanillaIdentifier,*/ @NotNull Identifier variantIdentifier) {//,TextureSource source) {
        //this.vanillaIdentifier = vanillaIdentifier;
        //this.source = source;
        this.thisIdentifier = variantIdentifier;
        Pattern pattern = Pattern.compile("\\d+(?=\\.png)");
        Matcher matcher = pattern.matcher(variantIdentifier.getPath());
        if (matcher.find()) {
            this.variantNumber = Integer.parseInt(matcher.group());
        } else {
            this.variantNumber = 0;
        }
        setupBlinking();
        setupEmissives();

    }

    //alternative initiator for already known textures used for players
    public ETFTexture(@NotNull Identifier modifiedSkinIdentifier,
                      @Nullable Identifier blinkIdentifier,
                      @Nullable Identifier blink2Identifier,
                      @Nullable Identifier emissiveIdentifier,
                      @Nullable Identifier blinkEmissiveIdentifier,
                      @Nullable Identifier blink2EmissiveIdentifier) {

        //ALL input already tested and confirmed existing
        this.variantNumber = 0;
        this.thisIdentifier = modifiedSkinIdentifier;
        this.blinkIdentifier = blinkIdentifier;
        this.blink2Identifier = blink2Identifier;
        this.emissiveIdentifier = emissiveIdentifier;
        this.emissiveBlinkIdentifier = blinkEmissiveIdentifier;
        this.emissiveBlink2Identifier = blink2EmissiveIdentifier;
        //setupBlinking(); neither required
        //setupEmissives();
        createPatchedTextures();
    }

    //alternative initiator for already known textures used for MooShroom's mushrooms
    public ETFTexture(@NotNull Identifier modifiedSkinIdentifier,
                      @Nullable Identifier emissiveIdentifier) {

        //ALL input already tested and confirmed existing
        this.variantNumber = 0;
        this.thisIdentifier = modifiedSkinIdentifier;
        this.emissiveIdentifier = emissiveIdentifier;
        //setupBlinking(); neither required
        //setupEmissives();
        createPatchedTextures();
    }


    private void setupBlinking() {
        try {
            if (ETFConfigData.enableBlinking) {
                ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

                if (ETFUtils2.isExistingResource(thisIdentifier)) {
                    Resource vanillaR1 = resourceManager.getResource(thisIdentifier);
                    Identifier possibleBlinkIdentifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink.png");
                    if (ETFUtils2.isExistingResource(possibleBlinkIdentifier)) {
                        String blink1PackName = resourceManager.getResource(possibleBlinkIdentifier).getResourcePackName();
                        ObjectSet<String> packs = new ObjectOpenHashSet<>();
                        packs.add(blink1PackName);
                        packs.add(vanillaR1.getResourcePackName());
                        if (blink1PackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                            //is higher or same pack
                            blinkIdentifier = possibleBlinkIdentifier;


                            Identifier possibleBlink2Identifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink2.png");
                            if (ETFUtils2.isExistingResource(possibleBlink2Identifier)) {
                                String blink2PackName = resourceManager.getResource(possibleBlink2Identifier).getResourcePackName();
                                if (blink1PackName.equals(blink2PackName)) {
                                    blink2Identifier = possibleBlink2Identifier;
                                }
                            }

                            //read possible blinking properties
                            Identifier propertyIdentifier = ETFUtils2.replaceIdentifier(possibleBlinkIdentifier, ".png", ".properties");
                            Properties blinkingProps = ETFUtils2.readAndReturnPropertiesElseNull(propertyIdentifier);
                            if (blinkingProps != null) {
                                if (ETFUtils2.isExistingResource(propertyIdentifier)) {
                                    String propertyResourcePackName = resourceManager.getResource(propertyIdentifier).getResourcePackName();
                                    packs.clear();
                                    packs.add(propertyResourcePackName);
                                    packs.add(blink1PackName);

                                    if (propertyResourcePackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                        blinkLength = blinkingProps.containsKey("blinkLength") ?
                                                Integer.parseInt(blinkingProps.getProperty("blinkLength").replaceAll("\\D", "")) :
                                                ETFConfigData.blinkLength;
                                        blinkFrequency = blinkingProps.containsKey("blinkFrequency") ?
                                                Integer.parseInt(blinkingProps.getProperty("blinkFrequency").replaceAll("\\D", "")) :
                                                ETFConfigData.blinkFrequency;

                                    }

                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
        //
        }
    }

    private void setupEmissives() {

        if (ETFConfigData.enableEmissiveTextures) {
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
            try {
                for (String possibleEmissiveSuffix :
                        EMISSIVE_SUFFIX_LIST) {

                    if (ETFUtils2.isExistingResource(thisIdentifier)) {
                        Resource vanillaR1 = resourceManager.getResource(thisIdentifier);
                        Identifier possibleEmissiveIdentifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", possibleEmissiveSuffix + ".png");
                        if (ETFUtils2.isExistingResource(possibleEmissiveIdentifier)) {

                            String emissivePackName = resourceManager.getResource(possibleEmissiveIdentifier).getResourcePackName();
                            ObjectSet<String> packs = new ObjectOpenHashSet<>();
                            packs.add(emissivePackName);
                            packs.add(vanillaR1.getResourcePackName());
                            if (emissivePackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                //is higher or same pack
                                emissiveIdentifier = possibleEmissiveIdentifier;
                                Identifier possibleEmissiveBlinkIdentifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink" + possibleEmissiveSuffix + ".png");
                                if (ETFUtils2.isExistingResource(possibleEmissiveBlinkIdentifier)) {

                                    String emissiveBlinkPackName = resourceManager.getResource(possibleEmissiveBlinkIdentifier).getResourcePackName();
                                    packs.clear();
                                    packs.add(emissiveBlinkPackName);
                                    packs.add(vanillaR1.getResourcePackName());
                                    if (emissiveBlinkPackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                        //is higher or same pack
                                        emissiveBlinkIdentifier = possibleEmissiveBlinkIdentifier;
                                        Identifier possibleEmissiveBlink2Identifier = ETFUtils2.replaceIdentifier(thisIdentifier, ".png", "_blink2" + possibleEmissiveSuffix + ".png");
                                        if (ETFUtils2.isExistingResource(possibleEmissiveBlink2Identifier)) {
                                            String emissiveBlink2PackName = resourceManager.getResource(possibleEmissiveBlink2Identifier).getResourcePackName();
                                            packs.clear();
                                            packs.add(emissiveBlink2PackName);
                                            packs.add(vanillaR1.getResourcePackName());
                                            if (emissiveBlink2PackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                                                //is higher or same pack
                                                emissiveBlink2Identifier = possibleEmissiveBlink2Identifier;
                                            }
                                        }
                                    }
                                }
                                //emissive found and is valid
                                break;
                            }
                        }
                    }
                }
            }catch(Exception e){
                //
            }
            if (isEmissive())
                createPatchedTextures();
        }
    }

    private void createPatchedTextures() {
        if(ETFVersionDifferenceHandler.isFabric() && ETFConfigData.temporary_fixIrisPBR){
            return;
        }
        //here we will 'patch' the base texture to prevent z-fighting with various shaders

        //null depending on existence
        NativeImage newBaseTexture = ETFUtils2.getNativeImageElseNull(thisIdentifier);
        NativeImage newBlinkTexture = ETFUtils2.getNativeImageElseNull(blinkIdentifier);
        NativeImage newBlink2Texture = ETFUtils2.getNativeImageElseNull(blink2Identifier);

        boolean didPatch = false;

        //we need to move iris pbr textures, these are texture_n.png   and texture_s.png
        //todo this does not currently work with iris pbr as currently textureManager registered textures are not valid for pbr
//
//        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
//            Identifier identifier_n = new Identifier(thisIdentifier.toString().replace(".png", "_n.png"));
//            Optional<Resource> base_nResource = manager.getResource(identifier_n);
//            if (base_nResource.isPresent()) {
//                NativeImage ntexture = ETFUtils2.getNativeImageElseNull(identifier_n);
//                ETFUtils2.registerNativeImageToIdentifier(ntexture, new Identifier(PATCH_NAMESPACE_PREFIX + identifier_n.getNamespace(), identifier_n.getPath()));
//                //System.out.println("one, "+identifier_n.toString());
//            }
//            Identifier identifier_s = new Identifier(thisIdentifier.toString().replace(".png", "_s.png"));
//            Optional<Resource> base_sResource = manager.getResource(identifier_s);
//            if (base_sResource.isPresent()) {
//                NativeImage stexture = ETFUtils2.getNativeImageElseNull(identifier_s);
//                ETFUtils2.registerNativeImageToIdentifier(stexture, new Identifier(PATCH_NAMESPACE_PREFIX + identifier_s.getNamespace(), identifier_s.getPath()));
//                //System.out.println("two, "+identifier_s.toString());
//            }
//
//            if (doesBlink()) {
//                Identifier identifier_blink_n = new Identifier(blinkIdentifier.toString().replace(".png", "_n.png"));
//                Optional<Resource> blink_nResource = manager.getResource(identifier_blink_n);
//                if (blink_nResource.isPresent()) {
//                    NativeImage blinkn = ETFUtils2.getNativeImageElseNull(identifier_blink_n);
//                    ETFUtils2.registerNativeImageToIdentifier(blinkn, new Identifier(PATCH_NAMESPACE_PREFIX + identifier_blink_n.getNamespace(), identifier_blink_n.getPath()));
//
//                }
//                Identifier identifier_blink_s = new Identifier(blinkIdentifier.toString().replace(".png", "_s.png"));
//                Optional<Resource> blink_sResource = manager.getResource(identifier_blink_s);
//                if (blink_sResource.isPresent()) {
//                    NativeImage blinks = ETFUtils2.getNativeImageElseNull(identifier_blink_s);
//                    ETFUtils2.registerNativeImageToIdentifier(blinks, new Identifier(PATCH_NAMESPACE_PREFIX + identifier_blink_s.getNamespace(), identifier_blink_s.getPath()));
//
//                }
//                if (doesBlink2()) {
//                    Identifier identifier_blink2_n = new Identifier(blink2Identifier.toString().replace(".png", "_n.png"));
//                    Optional<Resource> blink2_nResource = manager.getResource(identifier_blink2_n);
//                    if (blink2_nResource.isPresent()) {
//                        NativeImage blink2n = ETFUtils2.getNativeImageElseNull(identifier_blink2_n);
//                        ETFUtils2.registerNativeImageToIdentifier(blink2n, new Identifier(PATCH_NAMESPACE_PREFIX + identifier_blink2_n.getNamespace(), identifier_blink2_n.getPath()));
//
//                    }
//                    Identifier identifier_blink2_s = new Identifier(blink2Identifier.toString().replace(".png", "_s.png"));
//                    Optional<Resource> blink2_sResource = manager.getResource(identifier_blink2_s);
//                    if (blink2_sResource.isPresent()) {
//                        NativeImage blink2s = ETFUtils2.getNativeImageElseNull(identifier_blink2_s);
//                        ETFUtils2.registerNativeImageToIdentifier(blink2s, new Identifier(PATCH_NAMESPACE_PREFIX + identifier_blink2_s.getNamespace(), identifier_blink2_s.getPath()));
//
//                    }
//                }
//            }



        //patch out emissive textures for shader z fighting fix
        if (this.emissiveIdentifier != null && ETFConfigData.enableEmissiveTextures) {
            //create patched texture
            NativeImage emissiveImage = ETFUtils2.getNativeImageElseNull(emissiveIdentifier);
            try {
                patchTextureToRemoveZFightingWithOtherTexture(newBaseTexture, emissiveImage);
                didPatch = true;
                //no errors here means it all , and we have a patched texture in originalCopyToPatch
                //thisIdentifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + thisIdentifier.getNamespace(), thisIdentifier.getPath());
                //ETFUtils2.registerNativeImageToIdentifier(originalCopyToPatch, thisIdentifier_Patched);

                if (doesBlink() && emissiveBlinkIdentifier != null) {
                    NativeImage emissiveBlinkImage = ETFUtils2.getNativeImageElseNull(emissiveBlinkIdentifier);
                    patchTextureToRemoveZFightingWithOtherTexture(newBlinkTexture, emissiveBlinkImage);
                    //no errors here means it all worked, and we have a patched texture in
                    //blinkIdentifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + blinkIdentifier.getNamespace(), blinkIdentifier.getPath());
                    //ETFUtils2.registerNativeImageToIdentifier(blinkCopyToPatch, blinkIdentifier_Patched);

                    if (doesBlink2() && emissiveBlink2Identifier != null) {
                        NativeImage emissiveBlink2Image = ETFUtils2.getNativeImageElseNull(emissiveBlink2Identifier);
                        patchTextureToRemoveZFightingWithOtherTexture(newBlink2Texture, emissiveBlink2Image);
                        //no errors here means it all worked, and we have a patched texture in
                        //blink2Identifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + blink2Identifier.getNamespace(), blink2Identifier.getPath());
                        //ETFUtils2.registerNativeImageToIdentifier(blink2CopyToPatch, blinkIdentifier_Patched);
                    }
                }
            } catch (Exception ignored) {
                //
            }


            //save successful patches after any iris or other future patching reasons
            if (didPatch) {
                thisIdentifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + thisIdentifier.getNamespace(), thisIdentifier.getPath());
                ETFUtils2.registerNativeImageToIdentifier(newBaseTexture, thisIdentifier_Patched);
                if (doesBlink()) {
                    blinkIdentifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + blinkIdentifier.getNamespace(), blinkIdentifier.getPath());
                    ETFUtils2.registerNativeImageToIdentifier(newBlinkTexture, blinkIdentifier_Patched);
                    if (doesBlink2()) {
                        blink2Identifier_Patched = new Identifier(PATCH_NAMESPACE_PREFIX + blink2Identifier.getNamespace(), blink2Identifier.getPath());
                        ETFUtils2.registerNativeImageToIdentifier(newBlink2Texture, blink2Identifier_Patched);
                    }
                }
            }
        }
    }

    private void patchTextureToRemoveZFightingWithOtherTexture(NativeImage baseImage, NativeImage otherImage) throws IndexOutOfBoundsException {
        //here we alter the first image removing all pixels that are present in the second image to prevent z fighting
        //this does not support transparency and is a hard counter to f-fighting
        try {
            if (otherImage.getWidth() == baseImage.getWidth() && otherImage.getHeight() == baseImage.getHeight()) {
                //float widthMultipleEmissive  = originalCopy.getWidth()  / (float)emissive.getWidth();
                //float heightMultipleEmissive = originalCopy.getHeight() / (float)emissive.getHeight();

                for (int x = 0; x < baseImage.getWidth(); x++) {
                    for (int y = 0; y < baseImage.getHeight(); y++) {
                        //int newX = Math.min((int)(x*widthMultipleEmissive),originalCopy.getWidth()-1);
                        //int newY = Math.min((int)(y*heightMultipleEmissive),originalCopy.getHeight()-1);
                        if (otherImage.getPixelOpacity(x, y) != 0) {
                            baseImage.setPixelColor(x, y, 0);
                        }
                    }
                }
            }
            //return baseImage;
        } catch (Exception e) {
            throw new IndexOutOfBoundsException("additional texture is not the correct size, ETF has crashed in the patching stage");
        }
    }

    @NotNull
    Identifier getFeatureTexture(Identifier vanillaFeatureTexture) {

        if (FEATURE_TEXTURE_MAP.containsKey(vanillaFeatureTexture)) {
            return FEATURE_TEXTURE_MAP.get(vanillaFeatureTexture);
        }
        //otherwise we need to find what it is and add to map
        ETFDirectory directory = ETFDirectory.getDirectoryOf(thisIdentifier);
        if (variantNumber != 0) {
            Identifier possibleFeatureVariantIdentifier =
                    ETFDirectory.getIdentifierAsDirectory(
                            ETFUtils2.replaceIdentifier(
                                    vanillaFeatureTexture,
                                    ".png",
                                    variantNumber + ".png")
                            , directory);
            //Optional<Resource> possibleResource = MinecraftClient.getInstance().getResourceManager().getResource(possibleFeatureVariantIdentifier);
            if (ETFUtils2.isExistingResource(possibleFeatureVariantIdentifier)) {
                //feature variant exists so return
                FEATURE_TEXTURE_MAP.put(vanillaFeatureTexture, possibleFeatureVariantIdentifier);
                return possibleFeatureVariantIdentifier;
            }
        }
        //here we have no number and are likely vanilla texture or something went wrong in which case vanilla anyway
        //ETFUtils2.logWarn("getFeatureTexture() either vanilla or failed");
        ETFDirectory tryDirectory = ETFDirectory.getDirectoryOf(vanillaFeatureTexture);
        if (tryDirectory == directory || tryDirectory == ETFDirectory.VANILLA) {
            //if same directory as main texture or is vanilla texture use it
            Identifier tryDirectoryVariant = ETFDirectory.getIdentifierAsDirectory(vanillaFeatureTexture, tryDirectory);
            FEATURE_TEXTURE_MAP.put(vanillaFeatureTexture, tryDirectoryVariant);
            return tryDirectoryVariant;
        }
        //final fallback just use vanilla
        FEATURE_TEXTURE_MAP.put(vanillaFeatureTexture, vanillaFeatureTexture);
        return vanillaFeatureTexture;

    }

    @NotNull
    public Identifier getTextureIdentifier(LivingEntity entity) {
        return getTextureIdentifier(entity, false);
    }

    @NotNull
    public Identifier getTextureIdentifier(@Nullable LivingEntity entity, boolean forcePatchedTexture) {

        if (isPatched() && (forcePatchedTexture || (ETFConfigData.enableEmissiveTextures && ETFVersionDifferenceHandler.areShadersInUse()))) {
            //patched required
            currentTextureState = TextureReturnState.NORMAL_PATCHED;
            return getBlinkingIdentifier(entity);
        }
        currentTextureState = TextureReturnState.NORMAL;
        //regular required
        return getBlinkingIdentifier(entity);
    }

    @NotNull
    private Identifier getBlinkingIdentifier(@Nullable LivingEntity entity) {
        if (!doesBlink() || entity == null || !ETFConfigData.enableBlinking) {
            return identifierOfCurrentState();
        }

        //force eyes closed if asleep
        if (entity.getPose() == EntityPose.SLEEPING) {
            modifyTextureState(TextureReturnState.APPLY_BLINK);
            return identifierOfCurrentState();
        }
        //force eyes closed if blinded
        else if (entity.hasStatusEffect(StatusEffects.BLINDNESS)) {
            modifyTextureState(doesBlink2() ? TextureReturnState.APPLY_BLINK2 : TextureReturnState.APPLY_BLINK);
            return identifierOfCurrentState();
        } else {
            //do regular blinking
            if (entity.world != null) {
                UUID id = entity.getUuid();
                if (!ENTITY_BLINK_TIME.containsKey(id)) {
                    ENTITY_BLINK_TIME.put(id, entity.world.getTime() + blinkLength + 1);
                    return identifierOfCurrentState();
                }
                long nextBlink = ENTITY_BLINK_TIME.getLong(id);
                long currentTime = entity.world.getTime();

                if (currentTime >= nextBlink - blinkLength && currentTime <= nextBlink + blinkLength) {
                    if (doesBlink2()) {
                        if (currentTime >= nextBlink - (blinkLength / 3) && currentTime <= nextBlink + (blinkLength / 3)) {
                            modifyTextureState(TextureReturnState.APPLY_BLINK);
                            return identifierOfCurrentState();
                        }
                        modifyTextureState(TextureReturnState.APPLY_BLINK2);
                        return identifierOfCurrentState();
                    } else if (!(currentTime > nextBlink)) {
                        modifyTextureState(TextureReturnState.APPLY_BLINK);
                        return identifierOfCurrentState();
                    }
                } else if (currentTime > nextBlink + blinkLength) {
                    //calculate new next blink
                    ENTITY_BLINK_TIME.put(id, currentTime + entity.getRandom().nextInt(blinkFrequency) + 20);
                }
            }
        }
        return identifierOfCurrentState();
    }

    public boolean isEmissive() {
        return this.emissiveIdentifier != null;
    }

    public boolean isPatched() {
        return this.thisIdentifier_Patched != null;
    }

    public boolean doesBlink() {
        return this.blinkIdentifier != null;
    }


    public boolean doesBlink2() {
        return this.blink2Identifier != null;
    }

    @Override
    public String toString() {
        return "ETFTexture{texture=" + this.thisIdentifier.toString() +/*", vanilla="+this.vanillaIdentifier.toString()+*/", emissive=" + isEmissive() + ", patched=" + isPatched() + "}";
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ModelPart modelPart) {
        renderEmissive(matrixStack, vertexConsumerProvider, modelPart, ETFManager.getEmissiveMode());
    }


    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, ModelPart modelPart, ETFManager.EmissiveRenderModes modeToUsePossiblyManuallyChosen) {
        VertexConsumer vertexC = getEmissiveVertexConsumer(vertexConsumerProvider, null, modeToUsePossiblyManuallyChosen);
        if (vertexC != null) {
            modelPart.render(matrixStack, vertexC, ETFClientCommon.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        }
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Model model) {
        renderEmissive(matrixStack, vertexConsumerProvider, model, ETFManager.getEmissiveMode());
    }

    public void renderEmissive(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Model model, ETFManager.EmissiveRenderModes modeToUsePossiblyManuallyChosen) {
        VertexConsumer vertexC = getEmissiveVertexConsumer(vertexConsumerProvider, model, modeToUsePossiblyManuallyChosen);
        if (vertexC != null) {
            model.render(matrixStack, vertexC, ETFClientCommon.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        }
    }

    @Nullable
    public VertexConsumer getEmissiveVertexConsumer(VertexConsumerProvider vertexConsumerProvider, @Nullable Model model, ETFManager.EmissiveRenderModes modeToUsePossiblyManuallyChosen) {
        if (isEmissive()) {
            // block entity variants
            //removed in rework may return
//                if (irisDetected && ETFConfigData.fullBrightEmissives) {
//                    return vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(PATH_EMISSIVE_TEXTURE_IDENTIFIER.get(fileString), true));
//                } else {
//                    return vertexConsumerProvider.getBuffer(RenderLayer.getItemEntityTranslucentCull(PATH_EMISSIVE_TEXTURE_IDENTIFIER.get(fileString)));
//                }
            Identifier emissiveToUse = getEmissiveIdentifierOfCurrentState();
            if (emissiveToUse != null) {
                if (modeToUsePossiblyManuallyChosen == ETFManager.EmissiveRenderModes.BRIGHT) {
                    return vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissiveToUse, !ETFVersionDifferenceHandler.areShadersInUse()));
                } else {
                    if (model == null) {
                        return vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutoutNoCull /*RenderLayer.getEntityTranslucent*/(emissiveToUse));
                    } else {
                        return vertexConsumerProvider.getBuffer(model.getLayer(emissiveToUse));
                    }
                }
            }
        }
        //return null for any fail
        return null;
    }

    private void modifyTextureState(TextureReturnState givenState) {
        switch (givenState) {
            case APPLY_BLINK :
                    currentTextureState = currentTextureState == TextureReturnState.NORMAL_PATCHED ? TextureReturnState.BLINK_PATCHED : TextureReturnState.BLINK;
                    break;
            case APPLY_BLINK2 : switch (currentTextureState) {
                case NORMAL_PATCHED :
                case BLINK_PATCHED :
                    currentTextureState = TextureReturnState.BLINK2_PATCHED;break;
                default : currentTextureState = TextureReturnState.BLINK2;break;
            };
            //shouldn't ever call but may need in future
//            case APPLY_PATCH -> currentTextureState= switch (currentTextureState){
//                    case BLINK ->  TextureReturnState.BLINK_PATCHED;
//                    case BLINK2 -> TextureReturnState.BLINK2_PATCHED;
//                    default -> TextureReturnState.NORMAL_PATCHED;
//                };
            //default -> {}
        }
    }

    @NotNull
    private Identifier identifierOfCurrentState() {
         switch (currentTextureState) {
             case NORMAL :return thisIdentifier;
            case NORMAL_PATCHED :return thisIdentifier_Patched;
            case BLINK :return blinkIdentifier;
            case BLINK_PATCHED :return blinkIdentifier_Patched;
            case BLINK2 :return blink2Identifier;
            case BLINK2_PATCHED :return blink2Identifier_Patched;
            default :return
                //ETFUtils.logError("identifierOfCurrentState failed, it should not have, returning default");
                    thisIdentifier;
        }
    }

    @Nullable
    public Identifier getEmissiveIdentifierOfCurrentState() {
        switch (currentTextureState) {
            case NORMAL:case NORMAL_PATCHED :return emissiveIdentifier;
            case BLINK:case BLINK_PATCHED :return emissiveBlinkIdentifier;
            case BLINK2:case BLINK2_PATCHED :return emissiveBlink2Identifier;
            default :return
                //ETFUtils.logError("identifierOfCurrentState failed, it should not have, returning default");
                    null;
        }
    }

    public enum TextureReturnState {
        NORMAL,
        NORMAL_PATCHED,
        BLINK,
        BLINK_PATCHED,
        BLINK2,
        BLINK2_PATCHED,
        APPLY_PATCH,
        APPLY_BLINK,
        APPLY_BLINK2;


        @Override
        public String toString() {
            switch (this) {
                case NORMAL :return "normal";
                case BLINK :return "blink";
                case BLINK2 :return "blink2";
                case NORMAL_PATCHED :return "normal_patched";
                case BLINK_PATCHED :return "blink_patched";
                case BLINK2_PATCHED :return "blink2_patched";
                case APPLY_BLINK :return "apply_blink";
                case APPLY_BLINK2 :return "apply_blink2";
                case APPLY_PATCH :return "apply_patch";
                default:return "null";
            }
        }
    }

}
