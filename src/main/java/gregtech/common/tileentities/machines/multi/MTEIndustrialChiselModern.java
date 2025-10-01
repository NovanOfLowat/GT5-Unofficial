package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStreamUtil;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.pollution.PollutionConfig;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchChiselBus;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import team.chisel.carving.Carving;

public class MTEIndustrialChiselModern extends MTEExtendedPowerMultiBlockBase<MTEIndustrialChiselModern>
    implements ISurvivalConstructable {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final IStructureDefinition<MTEIndustrialChiselModern> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEIndustrialChiselModern>builder()
        .addShape(
            STRUCTURE_PIECE_MAIN,
            // spotless:off
            new String[][]{
                {"BBB", "BBB", "B~B", "BBB", "C C"},
                {"BBB", "A A", "A A", "BBB", "   "},
                {"BBB", "BAB", "BAB", "BBB", "C C"}
            })
        //spotless:on
        .addElement(
            'B',
            buildHatchAdder(MTEIndustrialChiselModern.class).atLeast(InputBus, OutputBus, Maintenance, Energy, Muffler)
                .casingIndex(Casings.PrinterCasing.textureId)
                .dot(1)
                .buildAndChain(
                    onElementPass(MTEIndustrialChiselModern::onCasingAdded, Casings.PrinterCasing.asElement())))
        .addElement('A', chainAllGlasses())
        .addElement('C', ofFrame(Materials.Steel))
        .build();

    private int mCasingAmount;
    private ItemStack target;
    private ItemStack mInputCache;
    private ItemStack mOutputCache;
    private GTRecipe mCachedRecipe;

    public MTEIndustrialChiselModern(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEIndustrialChiselModern(String aName) {
        super(aName);
    }

    @Override
    public IStructureDefinition<MTEIndustrialChiselModern> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEIndustrialChiselModern(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.PrinterCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCAIndustrialChiselActive)
                    .extFacing()
                    .build(),
                    TextureFactory.builder()
                        .addIcon(TexturesGtBlock.oMCAIndustrialChiselActiveGlow)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] { Casings.PrinterCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCAIndustrialChisel)
                    .extFacing()
                    .build(),
                    TextureFactory.builder()
                        .addIcon(TexturesGtBlock.oMCAIndustrialChiselGlow)
                        .extFacing()
                        .glow()
                        .build() };
            }
        } else {
            rTexture = new ITexture[] { Casings.PrinterCasing.getCasingTexture() };
        }
        return rTexture;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Chisel")
            .addInfo("300% faster than single block machines of the same voltage")
            .addInfo("Only uses 75% of the EU/t normally required")
            .addInfo("Processes sixteen items per voltage tier")
            .addInfo("Factory Grade Auto Chisel")
            .addInfo("Target block goes in Controller slot for common Input Buses")
            .addInfo("You can also set a target block in each Chisel Input Bus and use them as an Input Bus")
            .addInfo("If no target is provided for common buses, the result of the first chisel is used")
            .addPollutionAmount(PollutionConfig.pollutionPerSecondMultiIndustrialChisel)
            .beginStructureBlock(3, 5, 3, true)
            .addController("Front Center")
            .addCasingInfoMin("Sturdy Printer Casing", 6, false)
            .addCasingInfoExactly("Any Tiered Glass", 6, false)
            .addCasingInfoExactly("Steel Frame Box", 4, false)
            .addInputBus("Any Casing", 1)
            .addOutputBus("Any Casing", 1)
            .addEnergyHatch("Any Casing", 1)
            .addMaintenanceHatch("Any Casing", 1)
            .addMufflerHatch("Any Casing", 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 1, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 1, 2, 0, elementBudget, env, false, true);
    }

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasingAmount = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, 1, 2, 0)) return false;
        if (mMaintenanceHatches.isEmpty()) return false;
        if (mCasingAmount < 6) return false;
        return true;
    }

    private boolean hasValidCache(ItemStack input, ItemStack target, boolean checkTarget) {
        if (mInputCache == null || mOutputCache == null) return false;
        if (!GTUtility.areStacksEqual(input, mInputCache)) return false;
        if (checkTarget && !GTUtility.areStacksEqual(target, mOutputCache)) return false;
        return true;
    }

    private void cacheItem(ItemStack input, ItemStack output, GTRecipe recipe) {
        mInputCache = GTUtility.copyAmount(1, input);
        mOutputCache = GTUtility.copyAmount(1, output);
        mCachedRecipe = recipe;
    }

    private static boolean canBeMadeFrom(ItemStack from, ItemStack to) {
        if (from == null || to == null) return false;
        List<ItemStack> results = getItemsForChiseling(from);
        return results.stream()
            .anyMatch(stack -> GTUtility.areStacksEqual(stack, to));
    }

    private static List<ItemStack> getItemsForChiseling(ItemStack aStack) {
        return Carving.chisel.getItemsForChiseling(aStack);
    }

    private static ItemStack getChiselOutput(ItemStack aInput, ItemStack aTarget) {
        ItemStack tOutput;
        if (aTarget != null && canBeMadeFrom(aInput, aTarget)) {
            tOutput = aTarget;
        } else if (aTarget != null && !canBeMadeFrom(aInput, aTarget)) {
            tOutput = null;
        } else {
            tOutput = getItemsForChiseling(aInput).get(0);
        }
        return tOutput;
    }

    private GTRecipe generateChiselRecipe(ItemStack aInput) {
        boolean tIsCached = hasValidCache(aInput, this.target, true);
        if (tIsCached || aInput != null && !getItemsForChiseling(aInput).isEmpty()) {
            ItemStack tOutput = tIsCached ? mOutputCache.copy() : getChiselOutput(aInput, this.target);
            if (tOutput != null) {
                if (mCachedRecipe != null && GTUtility.areStacksEqual(aInput, mInputCache)
                    && GTUtility.areStacksEqual(tOutput, mOutputCache)) {
                    return mCachedRecipe;
                }
                GTRecipe aRecipe = new GTRecipe(
                    false,
                    new ItemStack[] { GTUtility.copyAmount(1, aInput) },
                    new ItemStack[] { GTUtility.copyAmount(1, tOutput) },
                    null,
                    new int[] { 10000 },
                    GTValues.emptyFluidStackArray,
                    GTValues.emptyFluidStackArray,
                    20,
                    16,
                    0);
                cacheItem(aInput, tOutput, aRecipe);
                return aRecipe;
            }
        }
        return null;
    }

    private GTRecipe getRecipe() {
        for (MTEHatchInputBus bus : this.mInputBusses) {
            if (bus instanceof MTEHatchChiselBus) {
                if (bus.mInventory[bus.getSizeInventory() - 1] == null) continue;
                this.target = bus.mInventory[bus.getSizeInventory() - 1];
                for (int i = bus.getSizeInventory() - 2; i >= 0; i--) {
                    ItemStack itemsInSlot = bus.mInventory[i];
                    if (itemsInSlot != null) {
                        GTRecipe tRecipe = generateChiselRecipe(itemsInSlot);
                        if (tRecipe != null) {
                            return tRecipe;
                        }
                    }
                }
            } else {
                target = this.getControllerSlot();
                for (int i = bus.getSizeInventory() - 1; i >= 0; i--) {
                    ItemStack itemsInSlot = bus.mInventory[i];
                    if (itemsInSlot != null) {
                        GTRecipe tRecipe = generateChiselRecipe(itemsInSlot);
                        if (tRecipe != null) {
                            return tRecipe;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Nonnull
            @Override
            protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
                return GTStreamUtil.ofNullable(getRecipe());
            }
        }.noRecipeCaching()
            .setSpeedBonus(1F / 3F)
            .setEuModifier(0.75F)
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    public int getMaxParallelRecipes() {
        return (16 * GTUtility.getTier(this.getMaxInputVoltage()));
    }

    @Override
    public int getRecipeCatalystPriority() {
        return -1;
    }

    private static ResourceLocation sChiselSound = null;

    private static ResourceLocation getChiselSound() {
        if (sChiselSound == null) {
            sChiselSound = new ResourceLocation(Carving.chisel.getVariationSound(Blocks.stone, 0));
        }
        return sChiselSound;
    }

    @Override
    protected void sendStartMultiBlockSoundLoop() {
        sendLoopStart(PROCESS_START_SOUND_INDEX);
    }

    @Override
    public void doSound(byte aIndex, double aX, double aY, double aZ) {
        switch (aIndex) {
            case PROCESS_START_SOUND_INDEX -> GTUtility
                .doSoundAtClient(getChiselSound(), getTimeBetweenProcessSounds(), 1.0F, 1.0F, aX, aY, aZ);
            case INTERRUPT_SOUND_INDEX -> GTUtility
                .doSoundAtClient(SoundResource.IC2_MACHINES_INTERRUPT_ONE, 100, 1.0F, aX, aY, aZ);
        }
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }

    @Override
    public int getPollutionPerSecond(ItemStack aStack) {
        return PollutionConfig.pollutionPerSecondMultiIndustrialChisel;
    }
}
