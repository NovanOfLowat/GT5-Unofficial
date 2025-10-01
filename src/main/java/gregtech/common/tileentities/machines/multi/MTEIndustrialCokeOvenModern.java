package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.casing.Casings;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.pollution.PollutionConfig;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;

public class MTEIndustrialCokeOvenModern extends MTEExtendedPowerMultiBlockBase<MTEIndustrialCokeOvenModern>
    implements ISurvivalConstructable {

    private static final int horizontalOffset = 1;
    private static final int verticalOffset = 2;
    private static final int depthOffset = 0;
    private int mTier = 0;
    private int mCasingAmount = 0;
    private static final String STRUCTURE_PIECE_T1 = "t1";
    private static final String STRUCTURE_PIECE_T2 = "t2";
    private static final IStructureDefinition<MTEIndustrialCokeOvenModern> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEIndustrialCokeOvenModern>builder()
        // spotless:off
        .addShape(
            STRUCTURE_PIECE_T1,
            transpose(
                new String[][] {
                    { "CCC", "CCC", "CCC" },
                    { "AAA", "A A", "AAA" },
                    { "C~C", "CCC", "CCC" }
                }))
        .addShape(
            STRUCTURE_PIECE_T2,
            transpose(
                new String[][] {
                    { "CCC", "CCC", "CCC" },
                    { "BBB", "B B", "BBB" },
                    { "C~C", "CCC", "CCC" }
                }))
            //spotless:on
        .addElement(
            'C',
            buildHatchAdder(MTEIndustrialCokeOvenModern.class)
                .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Maintenance, Energy, Muffler)
                .casingIndex(Casings.CokeOvenCasing.textureId)
                .dot(1)
                .buildAndChain(
                    onElementPass(MTEIndustrialCokeOvenModern::onCasingAdded, Casings.CokeOvenCasing.asElement())))
        .addElement('A', Casings.HeatResistantCokeOvenCasing.asElement())
        .addElement('B', Casings.HeatProofCokeOvenCasing.asElement())
        .build();

    public MTEIndustrialCokeOvenModern(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEIndustrialCokeOvenModern(String aName) {
        super(aName);
    }

    @Override
    public IStructureDefinition<MTEIndustrialCokeOvenModern> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEIndustrialCokeOvenModern(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] { Casings.CokeOvenCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCACokeOvenActive)
                    .extFacing()
                    .build(),
                    TextureFactory.builder()
                        .addIcon(TexturesGtBlock.oMCACokeOvenActiveGlow)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] { Casings.CokeOvenCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCACokeOven)
                    .extFacing()
                    .build(),
                    TextureFactory.builder()
                        .addIcon(TexturesGtBlock.oMCACokeOvenGlow)
                        .extFacing()
                        .glow()
                        .build() };
            }
        } else {
            rTexture = new ITexture[] { Casings.CokeOvenCasing.getCasingTexture() };
        }
        return rTexture;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Coke Oven")
            .addInfo("100% faster than single block machines of the same voltage")
            .addInfo("EU/t use reduced by 4% per voltage tier")
            .addInfo("Processes 12 items per level (Level 1: Heat Proof, Level 2: Heat Resistant)")
            .addPollutionAmount(PollutionConfig.pollutionPerSecondMultiIndustrialCokeOven)
            .beginStructureBlock(3, 5, 3, true)
            .addController("Front Center")
            .addCasingInfoMin("Heat Proof Machine Casings", 8, false)
            .addCasingInfoExactly("Heat Resistant Casings", 8, true)
            .addCasingInfoExactly("Any Tiered Glass", 6, false)
            .addCasingInfoExactly("Steel Frame Box", 4, false)
            .addInputBus("Any Casing", 1)
            .addOutputBus("Any Casing", 1)
            .addInputHatch("Any Casing", 1)
            .addOutputHatch("Any Casing", 1)
            .addEnergyHatch("Any Casing", 1)
            .addMaintenanceHatch("Any Casing", 1)
            .addMufflerHatch("Any Casing", 1)
            .toolTipFinisher();
        return tt;
    }

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public void construct(ItemStack holoStack, boolean hintsOnly) {
        if (holoStack.stackSize == 1) {
            buildPiece(STRUCTURE_PIECE_T1, holoStack, hintsOnly, horizontalOffset, verticalOffset, depthOffset);
        }
        if (holoStack.stackSize == 2) {
            buildPiece(STRUCTURE_PIECE_T2, holoStack, hintsOnly, horizontalOffset, verticalOffset, depthOffset);
        }
    }

    @Override
    public int survivalConstruct(ItemStack holoStack, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        if (holoStack.stackSize == 1) {
            return survivalBuildPiece(
                STRUCTURE_PIECE_T1,
                holoStack,
                horizontalOffset,
                verticalOffset,
                depthOffset,
                elementBudget,
                env,
                false,
                true);
        }
        if (holoStack.stackSize == 2) {
            return survivalBuildPiece(
                STRUCTURE_PIECE_T2,
                holoStack,
                horizontalOffset,
                verticalOffset,
                depthOffset,
                elementBudget,
                env,
                false,
                true);
        }
        return 0;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasingAmount = 0;
        mTier = 0;
        if (checkPiece(STRUCTURE_PIECE_T1, horizontalOffset, verticalOffset, depthOffset)) {
            mTier = 1;
        }
        mCasingAmount = 0;
        if (checkPiece(STRUCTURE_PIECE_T2, horizontalOffset, verticalOffset, depthOffset)) {
            mTier = 2;
        }
        if (mMaintenanceHatches.isEmpty()) return false;
        if (mMufflerHatches.isEmpty()) return false;
        if (mCasingAmount < 8) return false;
        return mTier > 0;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        logic.setEuModifier((100F - (GTUtility.getTier(getMaxInputVoltage()) * 4)) / 100F);
    }

    @Override
    public int getMaxParallelRecipes() {
        return this.mTier * 12;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTPPRecipeMaps.cokeOvenRecipes;
    }

    @Override
    public int getRecipeCatalystPriority() {
        return -1;
    }

    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.IC2_MACHINES_ELECTROFURNACE_LOOP;
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
        return PollutionConfig.pollutionPerSecondMultiIndustrialCokeOven;
    }
}
