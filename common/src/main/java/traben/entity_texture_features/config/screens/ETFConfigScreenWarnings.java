package traben.entity_texture_features.config.screens;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import traben.entity_texture_features.ETFClientCommon;
import traben.entity_texture_features.ETFVersionDifferenceHandler;

import java.util.Objects;

//inspired by puzzles custom gui code
public class ETFConfigScreenWarnings extends ETFConfigScreen {
    final ObjectOpenHashSet<ConfigWarning> warningsFound;

    protected ETFConfigScreenWarnings(Screen parent, ObjectOpenHashSet<ConfigWarning> warningsFound) {
        super(ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".general_settings.title"), parent);
        this.warningsFound = warningsFound;

    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(new ButtonWidget((int) (this.width * 0.55), (int) (this.height * 0.9), (int) (this.width * 0.2), 20,
                ScreenTexts.BACK,
                (button) -> Objects.requireNonNull(client).setScreen(parent)));
        this.addDrawableChild(new ButtonWidget((int) (this.width * 0.25), (int) (this.height * 0.9), (int) (this.width * 0.2), 20,
                ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".enable_all"),
                (button) -> {
                    //temporaryETFConfig = new ETFConfig();
                    ETFConfigScreenMain.temporaryETFConfig.ignoredConfigs.clear();
                    this.clearAndInit();
                    //Objects.requireNonNull(client).setScreen(parent);
                }));

        double offset = 0.0;
        for (ConfigWarning warning :
                warningsFound) {

            ButtonWidget butt = getETFButton((int) (this.width * 0.2), (int) (this.height * (0.3 + offset)), (int) (this.width * 0.6), 20,
                    warning.getButton_translation(),
                    (button) -> {
                        button.active = false;
                        ETFConfigScreenMain.temporaryETFConfig.ignoredConfigs.add(warning);
                    },
                    warning.getTooltip_translation()
            );
            butt.active = !ETFConfigScreenMain.temporaryETFConfig.ignoredConfigs.contains(warning);
            this.addDrawableChild(butt);

            offset += 0.1;
            //todo offset method only good for about 6 warnings, return here if adding more than 7 in future
        }


    }


    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        drawCenteredText(matrices, textRenderer, ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".warn_instruction"), (int) (width * 0.5), (int) (height * 0.20), 0xFFFFFF);
        drawCenteredText(matrices, textRenderer, ETFVersionDifferenceHandler.getTextFromTranslation("config." + ETFClientCommon.MOD_ID + ".warn_instruction2"), (int) (width * 0.5), (int) (height * 0.25), 0x888888);

    }

    public enum ConfigWarning {
        FIGURA("figura", "config." + ETFClientCommon.MOD_ID + ".figura_warn.button", "config." + ETFClientCommon.MOD_ID + ".figura_warn.text"),
        SKINLAYERS("skinlayers", "config." + ETFClientCommon.MOD_ID + ".skinlayers_warn.button", "config." + ETFClientCommon.MOD_ID + ".skinlayers_warn.text"),
        ENHANCED_BLOCK_ENTITIES("enhancedblockentities", "config." + ETFClientCommon.MOD_ID + ".ebe_warn.button", "config." + ETFClientCommon.MOD_ID + ".ebe_warn.text"),
        QUARK("quark", "config." + ETFClientCommon.MOD_ID + ".quark_warn.button", "config." + ETFClientCommon.MOD_ID + ".quark_warn.text");

        final private String mod_id;
        final private String button_translation_key;
        final private String tooltip_translation_key;

        ConfigWarning(String mod_id, String button_translation_key, String tooltip_translation_key) {
            this.mod_id = mod_id;
            this.button_translation_key = button_translation_key;
            this.tooltip_translation_key = tooltip_translation_key;
        }

        public String getMod_id() {
            return mod_id;
        }

        public Text getButton_translation() {
            return ETFVersionDifferenceHandler.getTextFromTranslation(button_translation_key);
        }

        public Text getTooltip_translation() {
            return ETFVersionDifferenceHandler.getTextFromTranslation(tooltip_translation_key);
        }


    }
}
